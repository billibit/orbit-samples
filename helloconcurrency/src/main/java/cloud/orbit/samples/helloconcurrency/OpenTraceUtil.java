package cloud.orbit.samples.helloconcurrency;

import java.util.ArrayList;

import cloud.orbit.actors.runtime.ActorTaskContext;
import cloud.orbit.actors.Stage;
import io.opentracing.Span;
import io.opentracing.propagation.Format;
import io.opentracing.util.GlobalTracer;

public class OpenTraceUtil {

	public OpenTraceUtil() {
		// TODO Auto-generated constructor stub
	}
	
	static public void enableTracing(Stage stage) {
        // Add the open tracing extension 
        stage.addExtension(new OpenTraceActorExtension());
        stage.addExtension(new OpenTraceInvocationHandlerExtension());       
        System.out.println("Stage: Add sticky headers");
        ArrayList<String> headers = new ArrayList<String>();
        headers.add( "SpanContext");
        headers.add( "ActorLifeTimeSpanContext");        
        stage.addStickyHeaders(headers);		
	}
	
	static public ActorTaskContext pushSpanContext(Span span) {
        ActorTextMap spanContext = new ActorTextMap();
        GlobalTracer.get().inject(span.context(), Format.Builtin.TEXT_MAP, spanContext);	        	
    	ActorTaskContext atc = ActorTaskContext.pushNew();
    	atc.setProperty( "SpanContext", spanContext);
    	
    	return atc;
	}
	
	static public void popSpanContext( ActorTaskContext atc) {
		atc.pop();
	}
}
