package com.discord_furz_bot;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.entities.Member;

public class DiscordBot extends ListenerAdapter{
    private static Random random;
    private static File soundFolder;
    static AudioPlayerManager playerManager;
    static AudioPlayer player;
    static TrackScheduler schedular;

    public static Collection<Timer> tasks = new ArrayList();

    public void playRandomSound() {

        File[] soundFiles = soundFolder.listFiles();
        File soundFile = soundFiles[random.nextInt(soundFiles.length)];

        String Test = soundFile.getAbsolutePath();
        System.out.println(Test);
        playerManager.loadItem(Test, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    player.startTrack(track, false);
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    AudioTrack track = playlist.getSelectedTrack();
                    if (track == null) {
                        track = playlist.getTracks().get(0);
                    }
                    player.startTrack(track, false);
                }

                @Override
                public void noMatches() {
                    // no matches
                    System.out.println("no matches");
                }

                @Override
                public void loadFailed(FriendlyException e) {
                    // loading failed
                    System.out.println("loading failed");
                }
            });
    }
           
    static class FurzTask extends TimerTask{
        private final DiscordBot bot;

        public FurzTask(DiscordBot bot) {
            this.bot = bot;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            bot.playRandomSound();
        }

    }

    public void scheduleRandomSound() {
        int delay = random.nextInt(5 * 60) + 1 * 60;
        delay = delay * 1000; // delay between 1 and 5 minutes
        System.out.println("delay: " + delay);
        FurzTask furzTask = new FurzTask(this);
        Timer furzTimer = new Timer();
        furzTimer.schedule(furzTask, delay);
        tasks.add(furzTimer);
    }

    public static void main(String[] args) throws Exception {
        String data = "Error";
        try {
            File myObj = new File("./discord_furz_bot/src/token.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                data = myReader.nextLine();
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        soundFolder = new File("discord_furz_bot/src/FurzSounds");

        DiscordBot bot = new DiscordBot();

        JDABuilder.createDefault(data) // Use token read from File
            .enableIntents(GatewayIntent.MESSAGE_CONTENT)
            .addEventListeners(bot) // Register new DiscordBot instance as EventListener
            .build(); // Build JDA - connect to discord

        playerManager = new DefaultAudioPlayerManager();
        player = playerManager.createPlayer();
        AudioSourceManagers.registerLocalSource(playerManager);

        schedular = new TrackScheduler(player, bot);
        player.addListener(schedular);

        random = new Random();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) 
    {
        // Make sure we only respond to events that occur in a guild
        if (!event.isFromGuild()) return;
        // This makes sure we only execute our code when someone sends a message with "!play"

        if (event.getMessage().getContentRaw().startsWith("!play")) {
            // Now we want to exclude messages from bots since we want to avoid command loops in chat!
            // this will include own messages as well for bot accounts
            // if this is not a bot make sure to check if this message is sent by yourself!
            if (event.getAuthor().isBot()) return;
            Guild guild = event.getGuild();
            Member member = event.getMember();
            if (!member.getVoiceState().inAudioChannel()) return;
            // This will get the first voice channel with the name "music"
            // matching by voiceChannel.getName().equalsIgnoreCase("music")
            //VoiceChannel channel = member.getVoiceState().getChannel(); //ByName("Talk 2", true).get(0);
            //System.out.println(channel);
            AudioManager manager = guild.getAudioManager();
            // MySendHandler should be your AudioSendHandler implementation
            manager.setSendingHandler(new AudioPlayerSendHandler(player));
            // Here we finally connect to the target voice channel 
            // and it will automatically start pulling the audio from the MySendHandler instance
            manager.openAudioConnection(member.getVoiceState().getChannel());
            scheduleRandomSound();
        } else if(event.getMessage().getContentRaw().startsWith("!stop")) {
            Guild guild = event.getGuild();
            AudioManager manager = guild.getAudioManager();
            for (Timer timer : tasks) {
                timer.cancel();
            }
            manager.closeAudioConnection();
        } else return;
    }
}
