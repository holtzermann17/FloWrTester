## jtester.py - run tests written in Java

# to install (py)jnius...
#
# sudo apt-get install cython
# git clone git@github.com:kivy/pyjnius.git
# cd pyjnius/
# sudo python setup.py install

## (Note, the order of the following incantations is significant.)

import os

javapath = ".:./flow/ccg/flow/tests/*"
os.environ['CLASSPATH'] = javapath

# This seems to be required, if we want to run the script from inside the api-integration directory...
# ... I.e. setting javapath to javapath = ".:../flow/ccg/flow/tests/*" doesn't work.
os.chdir("..")

# loading up PyJNIus
from jnius import autoclass

## Body:

# Now we can load up the class...
RegexTester = autoclass('ccg.flow.tests.RegexTester')
regextester = RegexTester()

# ... and run it like this:
print regextester.runTest(["hello.*(world)"])
print regextester.runTest(["hello.*(world"])

StringInListTester = autoclass('ccg.flow.tests.StringInList')
stringinlisttester = StringInListTester()

print stringinlisttester.runTest("foo",["foo", "bar", "baz"])
print stringinlisttester.runTest("quux",["foo", "bar", "baz"])

PositiveIntegerTester = autoclass('ccg.flow.tests.PositiveInteger')
positiveintegertester = PositiveIntegerTester()

print positiveintegertester.runTest(3)
print positiveintegertester.runTest(-3)

IntAsStringTester = autoclass('ccg.flow.tests.IntAsString')
intasstringtester = IntAsStringTester()

print intasstringtester.runTest("3")
print intasstringtester.runTest("c3po")

IntAsStringOrAllTester = autoclass('ccg.flow.tests.IntAsStringOrAll')
intasstringoralltester = IntAsStringOrAllTester()

print intasstringoralltester.runTest("3")
print intasstringoralltester.runTest("all")
print intasstringoralltester.runTest("c3po")

FloatInRangeTester = autoclass('ccg.flow.tests.FloatInRange')
floatinrangetester = FloatInRangeTester()

print floatinrangetester.runTest(3.5,3,7)
print floatinrangetester.runTest(2.5,3,7)

EachOneTester = autoclass('ccg.flow.tests.meta.EachOne')
eachonetester = EachOneTester()

print eachonetester.runTest("IsWord",["foo", "bar", "baz"])
print eachonetester.runTest("IsWord",["foo", "bar", "baz quux"])
