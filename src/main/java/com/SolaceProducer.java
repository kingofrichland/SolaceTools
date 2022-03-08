/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 *  Solace JMS 1.1 Examples: QueueProducer
 */
package com;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.solacesystems.jms.SolConnectionFactory;
import com.solacesystems.jms.SolJmsUtility;

/**
 * Sends a persistent message to a queue using Solace JMS API implementation.
 * 
 * The queue used for messages is created on the message broker.
 */
public class SolaceProducer {

    final String QUEUE_NAME = "Q/messager";
    
    @Parameter(names = "-host", description = "Colon-separated <host>:<port>")
    public String host;

    @Parameter(names = "-u", description = "Solace username")
    public String username;

    @Parameter(names = "-p", description = "Solace password")
    public String password;
    
    @Parameter(names = "-vpn", description = "VPN name")
    public String vpn;
    
    @Parameter(names = "-ssl", description = "TCP / TCPs mode")
    public boolean ssl = false;

    @Parameter(names = "-byte", description = "Text / Byte mode")
    public boolean isByteMode = false;

    @Parameter(names = "-loop", description = "Process one time / infinite loop")
    public boolean isLoopMode = false;

    @Parameter(names = "-queue", description = "Queue names")
    public String queue;    

    @Parameter(names = "-topic", description = "Topic name")
    public String topic;
    
    @Parameter(names = "-f", description = "File name")
    public String filepath;
    
    @Parameter(names = "--help", description = "Show this help menu", help = true)
    private boolean help = false;

    public boolean isTopicMode = false;
    public boolean isFileMode = false;
    
    public void run(String... args) throws Exception {

        String host = this.host;
        String vpn = this.vpn;
        String username = this.username;
        String password = this.password;
        String queuename = this.queue;   
        String topicname = this.topic;
        isTopicMode = topicname!=null && !"".equals(topicname);
        if (isTopicMode) System.out.println("topicname = ["+topicname+"]");
        isFileMode = filepath!=null && !"".equals(filepath);
        if (isFileMode) System.out.println("filepath = ["+filepath+"]");
        if (isFileMode) isLoopMode = false;

        System.out.printf("SolaceProducer is connecting to Solace messaging at %s...%n", host);

        // Programmatically create the connection factory using default settings
        SolConnectionFactory connectionFactory = SolJmsUtility.createConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setVPN(vpn);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);

        // Enables persistent queues or topic endpoints to be created dynamically
        // on the router, used when Session.createQueue() is called below
        connectionFactory.setDynamicDurables(true);
	    
	connectionFactory.setSSLValidateCertificate(false);

        // Create connection to the Solace router
        Connection connection = connectionFactory.createConnection();

        // Create a non-transacted, auto ACK session.
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        System.out.printf("Connected to the Solace Message VPN '%s' with client username '%s'.%n", vpn,
                username);

        // Create the queue programmatically and the corresponding router resource
        // will also be created dynamically because DynamicDurables is enabled.
        
        // From the session, create a consumer for the destination.
        MessageProducer messageProducer = null;
        Destination destination = null;
        if (isTopicMode) {
        	destination = session.createTopic(topicname);
        	messageProducer = session.createProducer(destination);
        }else {
        	destination = session.createQueue(queuename);
        	messageProducer = session.createProducer(destination);
        } 

        // User prompt, what is your text message??
        BufferedReader reader = null;
        String inputMessage = "";
        if (isFileMode) {
        	System.out.printf("Hello! Reading file["+filepath+"]: \n");
        	reader = new BufferedReader(new FileReader(filepath));
        }else {
        	System.out.printf("Hello! Enter message here: \n");
        	reader = new BufferedReader(new InputStreamReader(System.in));
        }

        while (inputMessage.isEmpty()) {
            System.out.printf("> ");
            if (isFileMode) {
            	inputMessage = reader.lines().collect(Collectors.joining());
            }else {
            	inputMessage = reader.readLine(); 
            }
        
	        Message message = null;
	        if (isByteMode) {
		        // Create a Byte message.
		        BytesMessage byteMessage = session.createBytesMessage();
		        byteMessage.writeBytes(inputMessage.getBytes());	
		        message = byteMessage;
	        }else {
	            // Create a text message.
		        TextMessage textMessage = session.createTextMessage(inputMessage);
		        message = textMessage;
	        }
	
	        // Send the message
	        // NOTE: JMS Message Priority is not supported by the Solace Message Bus
	        messageProducer.send(destination, message, DeliveryMode.PERSISTENT, Message.DEFAULT_PRIORITY,
	                Message.DEFAULT_TIME_TO_LIVE);
	
	        System.out.println("[SENT]");
	        
	        if (!isLoopMode) break;
	        if ("exit".equalsIgnoreCase(inputMessage)) break;
	        inputMessage = "";
	    }

        // Close everything in the order reversed from the opening order
        // NOTE: as the interfaces below extend AutoCloseable,
        // with them it's possible to use the "try-with-resources" Java statement
        // see details at https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
        messageProducer.close();
        session.close();
        connection.close();
    }

    /*
     * SolaceProducer -host localhost:55555 -u admin -p admin -vpn default -queue "Q/messager"
     * */
    public static void main(String... args) throws Exception {
        
    	try {
	        SolaceProducer jct = new SolaceProducer();
	        JCommander jcommander = JCommander.newBuilder()
	    	  .addObject(jct)
	    	  .build();
	        jcommander.parse(args);
	    	
	    	if (jct.help) {
	    		jcommander.usage();
	            return;
	        }
	
	        jct.run(args);
    	} catch (ParameterException e) {
    		System.err.println(e.getMessage());
    		System.err.println("Please use --help to show the help menu.");
    	}
    }
    
}
