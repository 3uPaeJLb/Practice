import reactor.netty.incubator.quic.QuicClient;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

public class QuicEchoClient {
    public static void main(String[] args) throws Exception {
        var clientCtx = QuicSslContextBuilder.forClient()
                .trustManager(io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocols("echo")
                .build();

        var client = QuicClient.create()
                .bindAddress(() -> new InetSocketAddress(0))
                .remoteAddress(() -> new InetSocketAddress("127.0.0.1", 4433))
                .secure(clientCtx)
                .idleTimeout(Duration.ofSeconds(10))
                .initialSettings(spec -> spec
                        .maxData(10_000_000)
                        .maxStreamDataBidirectionalLocal(1_000_000))
                .connectNow();

        var latch = new CountDownLatch(1);

        client.createStream((in, out) ->
                        out.sendString(reactor.core.publisher.Mono.just("Hello QUIC Server!"))
                                .then(in.receive().asString()
                                        .doOnNext(resp -> {
                                            System.out.println("[Client] Response: " + resp);
                                            latch.countDown();
                                        })
                                        .then()))
                .subscribe();

        latch.await();
        client.disposeNow();
    }
}