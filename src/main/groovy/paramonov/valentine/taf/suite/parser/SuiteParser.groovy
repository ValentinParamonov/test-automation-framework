package paramonov.valentine.taf.suite.parser

import freemind.Map
import freemind.Node
import paramonov.valentine.taf.suite.Request
import paramonov.valentine.taf.suite.Scenario
import paramonov.valentine.taf.suite.Suite
import paramonov.valentine.taf.suite.TestCase

import static TextMatcher.match

class SuiteParser {
    static final httpMethods = 'GET|HEAD|POST|PUT|DELETE|CONNECT|OPTIONS|TRACE'

    static Suite parse(Map suite) {
        new Suite(name: suite?.node?.TEXT,
                  scenarios: childNodes(suite?.node)?.collect { parseScenario(it) })
    }

    private static childNodes(Node node) {
        node?.arrowlinkOrCloudOrEdge?.findAll { it.class == Node }?.collect { it as Node } ?: []
    }

    private static Scenario parseScenario(Node scenario) {
        def childNodes = childNodes(scenario)
        new Scenario(name: scenario.TEXT, request: parseRequest(childNodes.first()), testCases: childNodes.tail().collect { parseTestCase(it) })
    }

    private static Request parseRequest(Node request) {
        match(request.TEXT).toPattern('Request')
                           .expect('a request node')
                           .matches()
        final childNodes = childNodes(request)
        if (childNodes.size() != 2) {
            throw new SuiteParseException('Expected the Request node to have two child nodes')
        }
        new Request(method: parseMethod(childNodes.first()), path: parsePath(childNodes.last()))
    }

    private static String parseMethod(Node method) {
        match(method.TEXT).toPattern(/^Method:\s*($httpMethods)$/)
                          .expect("Method: <$httpMethods>")
                          .matches()
                          .last()
    }

    private static String parsePath(Node path) {
        match(path.TEXT).toPattern(/^Path:\s*((?:\/\w+)*)$/)
                        .expect('Path: <path>')
                        .matches()
                        .last()
    }

    private static TestCase parseTestCase(Node testCase) {
        def childNodes = childNodes(testCase)
        new TestCase(description: match(testCase.TEXT).toPattern(/^Test:\s*(.+)$/)
                                                      .expect('Test: <name>')
                                                      .matches()
                                                      .last(),
                     parameters: childNodes.init().collectEntries { parseParameter(it) },
                     expectedResult: parseResult(childNodes.last()))
    }

    private static java.util.Map<String, String> parseParameter(Node parameter) {
        match(parameter.TEXT).toPattern(/^(\w+):\s*(.+)$/)
                             .expect('<parameterName>: <value>')
                             .matches()
                             .with { [(it[1]): it[2]] }
    }

    private static String parseResult(Node result) {
        match(result.TEXT).toPattern(/^result:\s*(.+)$/)
                          .expect('result: <expectedValue>')
                          .matches()
                          .last()
    }
}
