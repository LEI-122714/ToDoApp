package com.example.examplefeature;

import javazoom.jl.player.Player;
import java.io.InputStream;

// Comentário para fazer uma commit e testar o SonarQube. Parte 2

public class ReminderSound {

    /**
     * Toca um som de lembrete a partir do arquivo MP3 no resources
     */
    public static void playReminder() {
        try (InputStream is = ReminderSound.class.getResourceAsStream("/sounds/reminder.mp3")) {
            if (is != null) {
                Player player = new Player(is);
                new Thread(() -> {
                    try {
                        player.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                System.err.println("Arquivo de som não encontrado!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
