import os
import javabridge
 
javabridge.start_vm(class_path = [".", "../ccg/flow/*" ,"../ccg/flow/tests/*"] + javabridge.JARS, run_headless=True)
try:
 print javabridge.run_script('java.lang.String.format(System.getProperty("java.class.path"));')
 # print javabridge.run_script('ccg.flow.tests.RegexTester("(Hello).*(world)");')
finally:
 javabridge.kill_vm()
