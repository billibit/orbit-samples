/*
 Copyright (C) 2019 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package cloud.orbit.samples.helloconcurrency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.Stage.Builder;
import cloud.orbit.actors.runtime.ActorTaskContext;
import cloud.orbit.concurrent.Task;
import cloud.orbit.concurrent.TaskContext;
import io.jaegertracing.Configuration;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.util.GlobalTracer;

/**
 * Created by jianjunzhou@ea.com.
 * This demo is created from the helloworld example but it is designed to demo the concurrency features in the orbit framework
 */
public class Main
{
	static private void printActiveSpan() {
		Span activeSpan = GlobalTracer.get().activeSpan();
		System.out.println( "Active Trace Id = " + activeSpan.context().toTraceId() + " Span Id = " + activeSpan.context().toSpanId());		
	}

	static public void processMessagesByOneActor() {
		
        System.out.println("------------DEMO processing messages in sequence by one actor -----------------");
        final int total = 10;
        
        Span spanParent = GlobalTracer.get().buildSpan("processMessagesByOneActor")
				  .withTag("author", "zjj")
				  .withTag("app", "hello concurrency")
				  .start();
        
        spanParent.log("Loop starting");
        System.out.println( "Parent Trace Id = " + spanParent.context().toTraceId() + " Span Id = " + spanParent.context().toSpanId());
        
        try ( Scope scope = GlobalTracer.get().activateSpan(spanParent)) {
        	      
	        for( int i = 0; i < total; i++) {
	        	
	        	printActiveSpan();        		
	
	        	if ( i == total / 2) {
	        		
	                spanParent.log("Loop in the middle");	        		
	        	}
	        	
	            Span span = GlobalTracer.get()
	            		.buildSpan("action " + i)
	            		.asChildOf(spanParent)
	            		.start();
	            span.setTag("Kind", "Code block in Loop");

	            
	            try ( Scope scopeItem = GlobalTracer.get().activateSpan(span)) {      

		        	printActiveSpan();        		
	            	
	            	Span activeSpan = GlobalTracer.get().activeSpan();
		            System.out.println( "Child Trace Id = " + activeSpan.context().toTraceId() + " Span Id = " + activeSpan.context().toSpanId());
		            
		            ActorTaskContext atc = OpenTraceUtil.pushSpanContext( span);
		            
		        	String message = "Welcome to orbit " + i;
		            System.out.println("Message to send: " + message);
		            
		        	// Each message is processed by the actor 0, therefore all the messages are sent in sequence and processed in sequence too. The order of processing is opposite to the order of sending.	        		        	
		        	Task<String> task = Actor.getReference(Hello.class, "0").sayHello(message);		        	
		        	task.join();
		        	
		        	OpenTraceUtil.popSpanContext(atc);

	            } finally {
	            	span.finish();
	            }
	        } // for ...
        } finally {
        	spanParent.finish();
        }        
	}

	static public void processMessagesByMultipleActor() {
		
        Span spanParent = GlobalTracer.get().buildSpan("processMessagesByMultipleActor").start();
		
        System.out.println("------------DEMO processing messages in sequence by multiple actor intances -----------------");
        final int total = 10;
        for( int i = 0; i < total; i++) {
            Span span = GlobalTracer.get().buildSpan("action " + i).asChildOf(spanParent).start();	            
            try ( Scope scopeItem = GlobalTracer.get().activateSpan(span)) {    
            	
	            ActorTaskContext atc = OpenTraceUtil.pushSpanContext( span);
       	
	        	String message = "Welcome to orbit " + i;
	            System.out.println("Message to send: " + message);
	            
	        	// Each message is processed by a new instance of the HelloActor but in the order of receiving  
	        	Actor.getReference(Hello.class, String.format("%d", i)).sayHello(message).join();
	        	
	        	OpenTraceUtil.popSpanContext(atc);
            } finally {
            	span.finish();
            }
        }
	}
	
	static public void processMessagesConcurrently() {
		
        Span spanParent = GlobalTracer.get().buildSpan("processMessagesConcurrently").start();
        
        System.out.println("------------DEMO processing messages concurrently by multiple actor instances-----------------");
        // Send messages concurrently
        // The messages are sent in sequence but it is processed by actors concurrently, so the responses are received without orders.   
        final int total = 10;
        for( int i = 0; i < total; i++) {
            Span span = GlobalTracer.get().buildSpan("action " + i).asChildOf(spanParent).start();	            
            
            try ( Scope scopeItem = GlobalTracer.get().activateSpan(span)) {    
            	
            	ActorTaskContext atc = OpenTraceUtil.pushSpanContext(span);            	
	        	String message = "Welcome to orbit " + i;
	            System.out.println("Message to send: " + message);
	            
	            // Each message is processed by a new instance of the HelloActor
	        	Hello actor = Actor.getReference(Hello.class, String.format("%d", 0));
	        	Task<String> task = actor.sayHello(message);
	        	
	        	if ( i == total) {
	        		task.join();
	        	}
	        	
	        	OpenTraceUtil.popSpanContext(atc);
            } finally {
            	span.finish();
            }
        }
        
        spanParent.finish();
	}	
	
	static public void showMessageTimeoutException() {
		
        System.out.println("------------DEMO message timeout exception by one actor instance-----------------");

        if ( true)
        {
	    	String message = "Welcome to orbit 0";
	        System.out.println("Message to send: " + message);
	    	Actor.getReference(Hello.class, "0").sayHelloWithLongTimeToProcess(message);
        }
        {
	    	String message = "Welcome to orbit 1";
	        System.out.println("Message to send: " + message);
	    	Actor.getReference(Hello.class, "0").sayHello(message);
        }
        {
	    	String message = "Welcome to orbit 2";
	        System.out.println("Message to send: " + message);
	    	Actor.getReference(Hello.class, "0").sayHello(message);
        }

	}	
	
    public static void main(String[] args) throws Exception
    {

        // Create and bind to an orbit stage
    	Builder builder = new Stage.Builder();
    	builder.clusterName("orbit-demo-concurrency-cluster");
        Stage stage = builder.build();

        OpenTraceUtil.enableTracing( stage);

        Task<?> task = stage.start();
        task.join();
        stage.bind();
        

        // Setup the tracer
        Configuration config = Configuration.fromEnv();
        Tracer tracer = config.getTracer();
        GlobalTracer.registerIfAbsent(tracer);

        //SpanTest st = new SpanTest();        
        //st.processMessagesSayHelloOnce( 100);
    	processMessagesByOneActor();
    	//processMessagesConcurrently();
        //processMessagesByMultipleActor();
        //showMessageTimeoutException();

        // Shut down the stage
        stage.stop().join();
    }
}

