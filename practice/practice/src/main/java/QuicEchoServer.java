import io.netty.handler.ssl.util.SelfSignedCertificate;
import reactor.netty.incubator.quic.QuicServer;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.InsecureQuicTokenHandler;


import java.time.Duration;

public class QuicEchoServer {
    public static void main(String[] args) throws Exception {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        var serverCtx = QuicSslContextBuilder
                .forServer(ssc.privateKey(), null, ssc.certificate())
                .applicationProtocols("echo")
                .build();

        var server = QuicServer.create()
                .host("127.0.0.1")
                .port(4433)
                .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                .secure(serverCtx)
                .idleTimeout(Duration.ofSeconds(30))
                .initialSettings(spec -> spec
                        .maxData(10_000_000)
                        .maxStreamDataBidirectionalRemote(1_000_000)
                        .maxStreamsBidirectional(100))
                .handleStream((in, out) ->
                        out.sendString(in.receive().asString()
                                .map(s -> "Echo: " + s)))
                .bindNow();

        System.out.println("QUIC echo server started on 127.0.0.1:4433");
        server.onDispose().block();
    }
}