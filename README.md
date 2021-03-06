# TestTracer
Create test cases from activity diagrams.


WHAT IS TEST TRACER?

TestTracer is a simple tool that will create test case specifications from activity diagrams.

The main purpose is to define a set of usage scenarios that will cover all activity diagram transitions with the least amount of work.

The test case content will be based on the diagram nodes name, documentation and other metadata.
Obviously, the better the activity diagram is documented the more detailed the resulting test cases will be.
Additional text is added to remember the tester to verify situations like decisions, fork/joins, etc.

This tool feeds off XMI files (XML Metadata Interchange) exported form UML modeling applications (currently only supporting Enterprise Architect from Sparx Systems).

<a href="http://imgur.com/f20ERob"><img src="http://i.imgur.com/f20ERob.png" title="source: imgur.com" /></a>

HOW TO USE TEST TRACER?

- Download the latest executable JAR from https://github.com/joao-carloto/TestTracer/releases
- In Enterprise Architect, right click the package that contains the activity diagram(s) and select "Import/Export" > "Export package to XMI file"
- Run the TestTracer JAR.
- Select the XML file you just exported from EA.
- Click "Create Tests".
- If the exported package contains more than one activity diagram, select the one you pretend to process.
- The resulting test cases are displayed.

<a href="http://imgur.com/pOfwO1u"><img src="http://i.imgur.com/pOfwO1u.png" title="source: imgur.com" /></a>


CURRENT LIMITATIONS

This tool is currently at a "proof of concept" stage and hasn't been thoroughly tested (yes, the author is a tester, but has other responsibilities and limited time).
Some of the currently known limitations are:
- Text included in the tests, to handle decisions, fork/joins, etc, is written in English. Currently there's no interface to define a template in a different language.
- The test tracing algorithm has room for improvement and it has not been tested in really large and complex diagrams.
- The test cases produced are not in the typical step/result format, but as a sequence of actions, decisions and events, that may require additional editing.
- Some options on test case design may not be consensual. E.g. loops are only traveled once.
- It cannot be used on diagrams with elements copied as links to elements contained in another package.
- For now, it only supports XMI exported from EA.


Please feel free to contribute with suggestions and bug reports at https://github.com/joao-carloto/TestTracer/issues.
