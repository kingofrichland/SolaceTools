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
 *  Solace JMS 1.1 Examples: QueueConsumer
 */
package com;
import java.util.concurrent.CountDownLatch;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.solacesystems.jms.SolConnectionFactory;
import com.solacesystems.jms.SolJmsUtility;
import com.solacesystems.jms.SupportedProperty;

/**
 * Receives a persistent message from a queue using Solace JMS API implementation.
 *
 * The queue used for messages is created on the message broker.
 */
public class SolaceConsumer {

    final String QUEUE_NAME = "Q/messager";

    // Latch used for synchronizing between threads
    final CountDownLatch latch = new CountDownLatch(1);
    
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

    @Parameter(names = "-queue", description = "Queue name")
    public String queue;
    
    @Parameter(names = "-topic", description = "Topic name")
    public String topic;

    @Parameter(names = "--help", description = "Show this help menu", help = true)
    private boolean help = false;
    
    public boolean isTopicMode = false;
    
    public void run(String... args) throws Exception {

        String host = this.host;
        String vpn = this.vpn;
        String username = this.username;
        String password = this.password;
        String queuename = this.queue;
        String topicname = this.topic;
        System.out.println("topicname = ["+topicname+"]");
        isTopicMode = topicname!=null && !"".equals(topicname);

        System.out.printf("QueueConsumer is connecting to Solace messaging at %s...%n", host);

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

        // Create a non-transacted, client ACK session.
        Session session = connection.createSession(false, SupportedProperty.SOL_CLIENT_ACKNOWLEDGE);

        System.out.printf("Connected to the Solace Message VPN '%s' with client username '%s'.%n", vpn,
                username);

        // Create the queue programmatically and the corresponding router resource
        // will also be created dynamically because DynamicDurables is enabled.

        // From the session, create a consumer for the destination.
        MessageConsumer messageConsumer = null;
        Destination destination = null;
        if (isTopicMode) {
        	destination = session.createTopic(topicname);
        	messageConsumer = session.createConsumer(destination);
        }else {
        	destination = session.createQueue(queuename);
        	messageConsumer = session.createConsumer(destination);
        } 

        // Use the anonymous inner class for receiving messages asynchronously
        messageConsumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                try {
                	boolean unblock = false;
                    if (message instanceof TextMessage) {
                    	String recvMessage = ((TextMessage) message).getText();
                    	System.out.printf("[RECV]: [%s]%n", displayLimitString(recvMessage,200));
                    	unblock = "exit".equalsIgnoreCase(recvMessage);
                        // System.out.printf("TextMessage received: '%s'%n", ((TextMessage) message).getText());
                    } else if (message instanceof BytesMessage) {
                		BytesMessage byteMessage = (BytesMessage) message;
                		byte[] byteData = new byte[(int) byteMessage.getBodyLength()];
                		System.out.printf("[RECV]: BytesMessage length: %d%n",(int) byteMessage.getBodyLength());
                		byteMessage.readBytes(byteData);
                		byteMessage.reset();
                		String recvMessage = new String(byteData);
                    	System.out.printf("[RECV]: [%s]%n", displayLimitString(recvMessage,200));
                    	unblock = "exit".equalsIgnoreCase(recvMessage);
                    } else {
                        System.out.println("Message received.");
                    }
                    //System.out.printf("Message Content:%n%s%n", SolJmsUtility.dumpMessage(message));

                    // ACK the received message manually because of the set SupportedProperty.SOL_CLIENT_ACKNOWLEDGE above
                    message.acknowledge();

                    if (unblock) {
                    	latch.countDown(); // unblock the main thread
                    }
                } catch (JMSException ex) {
                    System.out.println("Error processing incoming message.");
                    ex.printStackTrace();
                }
            }
            
			private String displayLimitString(String recvMessage, int ilimit) {
				String str = (recvMessage==null)?"":recvMessage;
				if (str.length()>=ilimit) {
					str = str.substring(0, 0+ilimit/2) + "..." + str.substring(str.length()-ilimit/2);
				} 
				return str;
			}


        });

        // Start receiving messages
        connection.start();
        System.out.println("Awaiting message...");
        // the main thread blocks at the next statement until a message received
        latch.await();

        connection.stop();
        // Close everything in the order reversed from the opening order
        // NOTE: as the interfaces below extend AutoCloseable,
        // with them it's possible to use the "try-with-resources" Java statement
        // see details at https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
        messageConsumer.close();
        session.close();
        connection.close();
    }

    /*
     * SolaceConsumer -host localhost:55555 -u admin -p admin -vpn default -queue "Q/messager"
     * */
    public static void main(String... args) throws Exception {
		try {
			SolaceConsumer jct = new SolaceConsumer();
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
