package com.kroxaboom.skazka.download;

import java.net.URI;

public record HttpTransferResult(
        URI finalUri,
        int status,
        long bytes,
        boolean resumed,
        String contentType
) {}
