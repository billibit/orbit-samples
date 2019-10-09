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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.extensions.LifetimeExtension;
import cloud.orbit.actors.extensions.NamedPipelineExtension;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.ActorTaskContext;
import cloud.orbit.actors.runtime.DefaultHandlers;
import cloud.orbit.actors.runtime.Invocation;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.concurrent.Task;
import io.opentracing.Span;
import io.opentracing.propagation.Format;
import io.opentracing.util.GlobalTracer;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A pipeline Extension to collect the actor's metrics: number of actor, actor lifetimes distribution, actor message received rate
 *
 * Created by Jianjun Zhou on 10/08/2019.
 */
public class OpenTraceActorExtension implements LifetimeExtension
{
    private static final Logger logger = LoggerFactory.getLogger(OpenTraceActorExtension.class);

    public static final String ACTOR_METRICS_PIPELINE_NAME = "actor-metrics-pipeline";
    
    private Hashtable<String, Span> actorSpans = new Hashtable<String, Span>();
    
    private ActorTaskContext atc = null;

    public OpenTraceActorExtension()
    {
    }

    @Override
    public Task<?> preActivation(final AbstractActor<?> actor)
    {
        String spanName = getActorName( actor);        
        Span actorSpan = GlobalTracer.get().buildSpan(spanName).start();
        System.out.println("HelloActor::activateAsync: " + spanName + " span: " + actorSpan.context().toSpanId());        	   		
        actorSpans.put( spanName, actorSpan);
        
        ActorTextMap spanContext = new ActorTextMap();
        GlobalTracer.get().inject(actorSpan.context(), Format.Builtin.TEXT_MAP, spanContext);       
    	ActorTaskContext atc = ActorTaskContext.pushNew();
    	atc.setProperty( "ActorLifeTimeSpanContext", spanContext);
        
        actorSpan.log( "preActivation");
                       
        return Task.done();
    }

    @Override
    public Task<?> postActivation(final AbstractActor<?> actor)
    {
        String spanName = getActorName( actor);        
        Span actorSpan = actorSpans.get( spanName);
        System.out.println("HelloActor::activateAsync: " + spanName + " span: " + actorSpan.context().toSpanId());        	   		
        actorSpan.log( "postActivation");

        return Task.done();
    }

    @Override
    public Task<?> preDeactivation(final AbstractActor<?> actor)
    {
        String spanName = getActorName( actor);        
        Span actorSpan = actorSpans.get( spanName);
        System.out.println("HelloActor::activateAsync: " + spanName + " span: " + actorSpan.context().toSpanId());        	   		
        actorSpan.log( "preDeactivation");

        return Task.done();
    }

    @Override
    public Task<?> postDeactivation(final AbstractActor<?> actor)
    {
        String spanName = getActorName( actor);        
        Span actorSpan = actorSpans.get( spanName);
        System.out.println("HelloActor::activateAsync: " + spanName + " span: " + actorSpan.context().toSpanId());        	   		

        actorSpan.log( "postDeactivation");

        if  ( atc != null) {
        	atc.pop();
        }
        actorSpan.finish();
        
        return Task.done();
    }

    public static String getActorName( final AbstractActor<?> actor) {
        Object id = RemoteReference.getId(actor);
        return String.format("Actor.%s.%s", RemoteReference.getInterfaceClass(actor).getSimpleName(), id);    	
    }
    
}