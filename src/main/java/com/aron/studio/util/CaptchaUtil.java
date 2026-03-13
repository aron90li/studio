package com.aron.studio.util;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.util.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class CaptchaUtil {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Bean
    public Producer producer() {
        Properties props = new Properties();
        // 图片尺寸
        props.put("kaptcha.image.width", "120");
        props.put("kaptcha.image.height", "40");
        // 字符长度
        props.put("kaptcha.textproducer.char.length", "4");
        // 字体大小
        props.put("kaptcha.textproducer.font.size", "32");
        // 字体
        props.put("kaptcha.textproducer.font.names", "Arial,Courier");
        // 干扰线
        props.put("kaptcha.noise.impl", "com.google.code.kaptcha.impl.DefaultNoise");
        // 无干扰线
        // props.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.NoNoise");
        // 背景颜色
        props.put("kaptcha.background.clear.from", "white");
        props.put("kaptcha.background.clear.to", "white");
        // 文字颜色
        props.put("kaptcha.textproducer.font.color", "black");
        // 关键：关闭边框
        props.setProperty("kaptcha.border", "no");
        Config config = new Config(props);
        return config.getProducerImpl();
    }

    public Map<String, String> generateCaptcha(Producer producer) throws IOException {
        String uuid = UUID.randomUUID().toString();
        String code = producer.createText();
        BufferedImage image = producer.createImage(code);

        redisTemplate.opsForValue().set("captcha:" + uuid, code, 5, TimeUnit.MINUTES);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        String base64 = Base64.getEncoder().encodeToString(out.toByteArray());
        return Map.of("uuid", uuid, "image", base64);
    }

    public void validateCaptcha(String uuid, String code) {
        String key = "captcha:" + uuid;
        String real = redisTemplate.opsForValue().get(key);
        if (real == null || !real.equalsIgnoreCase(code)) {
            throw new RuntimeException("验证码错误或者过期");
        }
        redisTemplate.delete(key);
    }

}
