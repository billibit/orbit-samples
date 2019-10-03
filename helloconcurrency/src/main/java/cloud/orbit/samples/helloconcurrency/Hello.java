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

import java.util.concurrent.TimeUnit;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.annotation.Timeout;
import cloud.orbit.concurrent.Task;
import io.opentracing.Span;

/**
 * Created by joe@bioware.com on 2016-04-26.
 */
public interface Hello extends Actor
{
	// Set an active span for the actor. There is one instance of the actor so does the active span
	// TODO : Remove
	//Task<String> setActiveSpan(Span span);
	//Task<String> getActiveSpan();
	
    // Set the timeout annotation for this method in the actor
    @Timeout(value = 4000, timeUnit = TimeUnit.MILLISECONDS)
    Task<String> sayHello(String greeting);
    
    @Timeout(value = 4000, timeUnit = TimeUnit.MILLISECONDS)
    Task<String> sayHelloWithTrace(String greeting, ActorTextMap spanContext);
    
    // A method to say hello with a long waiting time
    Task<String> sayHelloWithLongTimeToProcess(String greeting);
}
