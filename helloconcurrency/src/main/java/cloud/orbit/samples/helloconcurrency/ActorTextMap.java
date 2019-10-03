package cloud.orbit.samples.helloconcurrency;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.opentracing.propagation.TextMap;

public class ActorTextMap implements TextMap  
{
    protected final Map<String, String> map = new HashMap<String, String>();

    public ActorTextMap() {
    }

    @Override
    public void put(String key, String value) {
        this.map.put(key, value);
    }

	@Override
	public Iterator<Entry<String, String>> iterator() {
        return map.entrySet().iterator();
	}
}
