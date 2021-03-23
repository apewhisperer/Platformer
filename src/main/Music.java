package main;

public class Music extends Thread {
    @Override
    public void run() {
        while (true) {
            GamePanel.playSound("tune");
            try {
                Thread.sleep(264000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
