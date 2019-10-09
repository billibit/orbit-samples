/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

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

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.extensions.InvocationHandlerExtension;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.ActorTaskContext;
import cloud.orbit.actors.runtime.Invocation;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.concurrent.Task;
import cloud.orbit.concurrent.TaskContext;
import cloud.orbit.exception.UncheckedException;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.util.GlobalTracer;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class OpenTraceInvocationHandlerExtension implements InvocationHandlerExtension
{
    private AtomicBoolean acceptCalls = new AtomicBoolean(true);

    // Keep the current span and the active span ( scope)
	private Span currentInvokeSpan = null;  	
	private Scope currentInvokeScope = null;

	private Span actorLifeTimeSpan = null;  	
	private Scope actorLifeTimeScope = null;
	
	// Generate the span context from the header/property of the TaskContext
	private SpanContext getSpanContext() {
		
		SpanContext spanContext = null; 
		
		TaskContext cp = TaskContext.current();
		Object data = cp.getProperty( "SpanContext");
		
		if  ( data != null ) {
			ActorTextMap spanContextData = (ActorTextMap)data;
			spanContext = GlobalTracer.get().extract(Format.Builtin.TEXT_MAP, spanContextData);		        
		}
		return spanContext;
	}
	
	//  Create and active the Span
	private void createSpan(Actor actor, String methodName) {
			    	
    	Span span = null;
		String spanName = getActorName(actor);
    	SpanContext sc = getSpanContext();
    	if ( sc != null) {
    		span = GlobalTracer.get().buildSpan(spanName).asChildOf( sc).start();
    	} else {
    		span = GlobalTracer.get().buildSpan(spanName).start();    		
    	}
        span.setTag("Kind", "Orbit Actor");
        
        currentInvokeSpan = span;
    	currentInvokeScope = GlobalTracer.get().activateSpan( currentInvokeSpan); 	
	}	
    
	// End the current active span
	private void endSpan() {
        if ( currentInvokeScope != null) {
        	currentInvokeScope.close();
        }
    	if ( currentInvokeSpan != null) {
    		currentInvokeSpan.finish();
    	}
    	currentInvokeScope = null;
    	currentInvokeSpan = null;
	}
	
	
	// Get the actor full name as the span name
    private String getActorName( final Actor actor) {
        return actor.getClass().getTypeName() + "." + actor.getIdentity();    	
    }
	
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    public Task beforeInvoke(final long startTimeNanos, final Object targetObject, final Method targetMethod, final Object[] params, Map<?, ?> invocationHeaders)
    {
        if(!acceptCalls.get()) throw new UncheckedException("Not accepting calls");
        
    	currentInvokeSpan = null;
    	currentInvokeScope = null;
    	
    	if ( targetObject != null && (targetObject instanceof Actor)) {
    		
        	createSpan((Actor)targetObject, targetMethod.getName());
	    }
		                	
        return Task.done();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    public Task afterInvoke(final long startTimeNanos, final Object targetObject, final Method targetMethod, final Object[] params, final Map<?, ?> invocationHeaders)
    {
        if(!acceptCalls.get()) throw new UncheckedException("Not accepting calls");
    	
        endSpan();
        
        return Task.done();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    public Task afterInvokeChain(final long startTimeNanos, final Object targetObject, final Method targetMethod, final Object[] params, final Map<?, ?> invocationHeaders)
    {
        return Task.done();
    }
}
