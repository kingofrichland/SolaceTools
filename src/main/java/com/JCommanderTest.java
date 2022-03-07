package com;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;

public class JCommanderTest {
	
    @Parameter
    public List<String> parameters = Lists.newArrayList();
 
    @Parameter(names = { "-log", "-verbose" }, description = "Level of verbosity")
    public Integer verbose = 1;
 
    @Parameter(names = "-groups", description = "Comma-separated list of group names to be run")
    public String groups;
 
    @Parameter(names = "-debug", description = "Debug mode")
    public boolean debug = false;

    @DynamicParameter(names = "-D", description = "Dynamic parameters go here")
    public Map<String, String> dynamicParams = new HashMap<String, String>();
    
    @Parameter(names = "-host", description = "Colon-separated list of group names to be run")
    public String host;
    
    @Parameter(names = "-vpn", description = "VPN name")
    public String vpn;
    
    @Parameter(names = "-ssl", description = "TCP / TCPs mode")
    public boolean ssl = false;

    @Parameter(names = "-byte", description = "Text / Byte mode")
    public boolean byteMode = false;

    @Parameter(names = "-loop", description = "Process one time / infinite loop")
    public boolean loop = false;

    @Parameter(names = "-queue", description = "Queue names")
    public String queue;
    
	@Test
	void test() {
    	JCommanderTest jct = new JCommanderTest();
    	String[] argv = { "-log", "2", "-groups", "unit1,unit2,unit3",
                "-debug", "-Doption=value", "a", "b", "c" };
    	JCommander.newBuilder()
    	  .addObject(jct)
    	  .build()
    	  .parse(argv);

    	Assert.assertEquals(2, jct.verbose.intValue());
    	Assert.assertEquals("unit1,unit2,unit3", jct.groups);
    	Assert.assertEquals(true, jct.debug);
    	Assert.assertEquals("value", jct.dynamicParams.get("option"));
    	Assert.assertEquals(Arrays.asList("a", "b", "c"), jct.parameters);
	}
	
	@Test
	void testSolaceClient() {
    	JCommanderTest jct = new JCommanderTest();
    	String[] argv = {  "-host", "localhost:55555", "-queue", "queue1", "-vpn", "dev",
    	                    "-ssl", "-loop" ,"-byte"};
    	JCommander.newBuilder()
    	  .addObject(jct)
    	  .build()
    	  .parse(argv);
    	
    	Assert.assertEquals(true, jct.ssl);
    	Assert.assertEquals(true, jct.byteMode);
    	Assert.assertEquals(true, jct.loop);
    	Assert.assertEquals("queue1", jct.queue);
    	Assert.assertEquals("localhost:55555", jct.host);
    	Assert.assertEquals("dev", jct.vpn);
    	
         	
    		
	}
	
}