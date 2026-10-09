package com.swna.javafx.backup.domain;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Environment {

    private Long id;

    private String backupFolder;
    private String backupExe;
    private String reportFolder;

    public static Environment create(
            String backupFolder,
            String backupExe,
            String reportFolder
    ) {
        Environment env = new Environment();
        env.backupFolder = backupFolder;
        env.backupExe = backupExe;
        env.reportFolder = reportFolder;

        return env;
    }

    public void setId(Long id) {
        this.id = id;
    }
}