package com.aron.studio;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommonTest {

    @Test
    public void testCommon() {
        LocalDateTime localDateTime = LocalDateTime.now();
        System.out.println("ISO_DATE_TIME:  " + localDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        System.out.println("ISO_DATE:       " + localDateTime.format(DateTimeFormatter.ISO_DATE));
        System.out.println("BASIC_ISO_DATE: " + localDateTime.format(DateTimeFormatter.BASIC_ISO_DATE));
        System.out.println("pattern:        " + localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }


}