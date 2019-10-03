/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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

import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.util.GlobalTracer;

import java.util.concurrent.TimeUnit;

/**
 * Created by joe@bioware.com on 2016-04-26.
 */
public class HelloActor extends AbstractActor implements Hello
{
	private Span parentSpan = null;  
    public HelloActor() {
    }
    
	public void setActiveSpan(Span span) {
		this.parentSpan = span;
	}
	
	public Span getActiveSpan() {
		return this.parentSpan;
	}

	@Override
    public Task activateAsync()
    {
        String spanName = this.getClass().toString() + this.getIdentity();        
        parentSpan = GlobalTracer.get().buildSpan(spanName).start();
        
        System.out.println("HelloActor::activateAsync: " + spanName + " span: " + parentSpan.context().toSpanId());        	   		
                
        return super.activateAsync();
    }

    @Override
    public Task deactivateAsync()
    {
    	if ( parentSpan != null) {
            String spanName = this.getClass().toString() + this.getIdentity();        
            System.out.println("HelloActor::deactivateAsync: " + spanName + " span: " + parentSpan.context().toSpanId());        	   		
    		parentSpan.finish();
    	}
        return super.deactivateAsync();
    }
        
	public Task<String> sayHello(String greeting)
    {	
        Span span = GlobalTracer.get()
        		.buildSpan("HelloActor:: " + "sayHello")
        		.asChildOf(this.getActiveSpan())
        		.start();	            
		
        try ( Scope scopeItem = GlobalTracer.get().activateSpan( span)) {
	    	try {
	    		// Sleep X ms and then send the message back
	            long delay = (long)(50);
	    		TimeUnit.MILLISECONDS.sleep( delay);
	    	} catch( Exception ex) {
	            System.out.println("Exception: " + ex.getMessage() + " Cause: " + ex.getCause());        	   		
	    	}
	    	
	    	System.out.println("Actor: " + greeting);
	
	        return Task.fromValue("You said: '" + greeting
	                + "', I say: Hello from " + System.identityHashCode(this) + " !");
	    } finally {
	    	span.finish();
	    }
        
    }
	
	//
	// Can't pass a Span as param as a Span is not cloneable.
	// The following will throw an exception at runtime
	// public Task<String> sayHelloWithTrace(String greeting, Span parentSpan)
	//
	public Task<String> sayHelloWithTrace(String greeting, ActorTextMap spanContext)
	{
        SpanContext sp = GlobalTracer.get().extract(Format.Builtin.TEXT_MAP, spanContext);
        Span span = null;
        if ( sp != null) {
        	span = GlobalTracer.get().buildSpan("HelloActor::" + "sayHello").asChildOf( sp).start();
        } else {
        	span = GlobalTracer.get().buildSpan("HelloActor::" + "sayHello").asChildOf( this.getActiveSpan()).start();
        }
		
        span.setTag("Kind", "Orbit Actor");
        
        try ( Scope scopeItem = GlobalTracer.get().activateSpan( span)) {
	    	try {
	    		// Sleep X ms and then send the message back
	            long delay = (long)(50);
	    		TimeUnit.MILLISECONDS.sleep( delay);
	    	} catch( Exception ex) {
	            System.out.println("Exception: " + ex.getMessage() + " Cause: " + ex.getCause());        	   		
	    	}
	    	
	    	System.out.println("Actor: " + greeting);
	
	        return Task.fromValue("You said: '" + greeting
	                + "', I say: Hello from " + System.identityHashCode(this) + " !");
	    } finally {
	    	span.finish();
	    }
	}
	
	public Task<String> sayHelloWithLongTimeToProcess(String greeting) {
        Span span = GlobalTracer.get().buildSpan("HelloActor::" + "sayHelloWithLongTimeToProcess").asChildOf(this.getActiveSpan()).start();	            
		
        try ( Scope scopeItem = GlobalTracer.get().activateSpan( span)) {
	    	try {
	    		// Sleep X ms and then send the message back
	            long delay = (long)(Math.random()*1000*100);
	    		TimeUnit.MILLISECONDS.sleep( delay);
	    	} catch( Exception ex) {
	            System.out.println("Exception: " + ex.getMessage() + " Cause: " + ex.getCause());        	   		
	    	}
	    	
	    	//System.out.println("Here: " + greeting);
	
	        return Task.fromValue("You said: '" + greeting
	                + "', I say: Hello from " + System.identityHashCode(this) + " !");    
		    } finally {
		    	span.finish();
	    }
    }
}
