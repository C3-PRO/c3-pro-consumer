package org.bch.c3pro.consumer.util;

/**
 * Interface for http responses
 * @author CHIP-IHL
 */
public interface Response {
    public int getResponseCode();
    public String getContent();
}
