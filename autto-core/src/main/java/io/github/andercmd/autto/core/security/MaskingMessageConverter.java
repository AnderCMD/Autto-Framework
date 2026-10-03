package io.github.andercmd.autto.core.security;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Logback converter that masks registered {@link Secrets} in log messages.
 *
 * <pre>{@code
 * <conversionRule conversionWord="maskedMsg"
 *                 class="io.github.andercmd.autto.core.security.MaskingMessageConverter"/>
 * <pattern>%d %-5level %logger - %maskedMsg%n</pattern>
 * }</pre>
 */
public class MaskingMessageConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return Secrets.mask(super.convert(event));
    }
}
