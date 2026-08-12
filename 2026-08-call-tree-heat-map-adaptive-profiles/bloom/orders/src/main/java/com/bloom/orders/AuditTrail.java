package com.bloom.orders;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AuditTrail {

    private final FileChannel channel;

    public AuditTrail(@Value("${bloom.audit.path}") String path) throws IOException {
        channel = new FileOutputStream(path, true).getChannel();
    }

    // Synchronized so audit entries stay strictly ordered across threads.
    public synchronized void record(String event, String details) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        String line = timestamp + " | " + event + " | " + details + "\n";
        try {
            channel.write(ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8)));
            // Audit entries must survive a crash.
            channel.force(false);
        } catch (IOException e) {
            throw new UncheckedIOException("audit trail write failed", e);
        }
    }
}
