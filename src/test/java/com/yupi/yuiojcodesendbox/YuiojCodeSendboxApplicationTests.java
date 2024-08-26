package com.yupi.yuiojcodesendbox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootTest
class YuiojCodeSendboxApplicationTests {

    @Test
    void contextLoads() {
//        String userDir = System.getProperty("user.dir");
//        String filePath = userDir + File.separator + "src/main/resources/application.yml";
//        try {
//            List<String> allLines = Files.readAllLines(Paths.get(filePath));
//            System.out.println(String.join("\n",allLines));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        String stringArgs = "1  3";
        String[] strings = stringArgs.split(" ");
        System.out.println(strings);

    }

}
