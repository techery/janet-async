import com.google.gson.Gson;

import org.junit.Test;

import io.techery.janet.AsyncActionService;
import io.techery.janet.AsyncClient;
import io.techery.janet.gson.GsonConverter;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class AsyncActionServiceTests {

    @Test
    public void testAsyncActionServiceCreating() {

        final GsonConverter converter = new GsonConverter(new Gson());
        final String url = "url";
        final AsyncClient client = mock(AsyncClient.class);

        final AsyncActionService service = new AsyncActionService(url, client, converter);

        assertThat(service, notNullValue());
    }
}
