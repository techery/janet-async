import org.junit.Test;

import io.techery.janet.AsyncActionService;
import io.techery.janet.AsyncClient;
import io.techery.janet.async.protocol.AsyncProtocol;
import io.techery.janet.converter.Converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AsyncActionServiceTests {

    @Test
    public void testAsyncActionServiceCreation() {
        String url = "mock_url";
        AsyncClient client = mock(AsyncClient.class);
        Converter converter = mock(Converter.class);

        AsyncActionService service = new AsyncActionService(url, client, new AsyncProtocol.Builder().build(), converter);

        assertThat(service).isNotNull();
    }
}
