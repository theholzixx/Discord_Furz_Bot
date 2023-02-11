package com.discord_furz_bot;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.audio.AudioSendHandler;

public class DiscordBot {
    private final JDA jda;
    private final Random random;
    private final File soundFolder;

    public DiscordBot(String discordToken, File soundFolder) throws Exception {
        this.jda = new JDABuilder(discordToken).build();
        this.random = new Random();
        this.soundFolder = soundFolder;
        this.jda.addEventListener(new BotEventListener(this));
    }//test ob git verbunden ist.

    public void playRandomSound(Guild guild) {
        VoiceChannel voiceChannel = guild.getVoiceChannels().get(0); // assuming you want to play the sound in the first voice channel
        File[] soundFiles = soundFolder.listFiles();
        File soundFile = soundFiles[random.nextInt(soundFiles.length)];
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioPlayer player = playerManager.createPlayer();
        guild.getAudioManager().setSendingHandler(new AudioSendHandler(player));
        try {
            playerManager.loadItem(soundFile.toURI().toURL().toString(), new AudioLoadResultHandler() {
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
                }
                @Override
                public void loadFailed(FriendlyException e) {
                    // loading failed
                }
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void scheduleRandomSound(Guild guild) {
        int delay = random.nextInt(5 * 60) + 1 * 60; // delay between 1 and 5 minutes
        guild.getJDA().getScheduler().schedule(() -> playRandomSound(guild), delay, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        File soundFolder = new File("path/to/sound/folder");
        DiscordBot bot = new DiscordBot("discord-bot-token", soundFolder);
    }

    private class BotEventListener extends ListenerAdapter {
        private final DiscordBot bot;

        public BotEventListener(DiscordBot bot) {
            this.bot = bot;
        }

        @Override
        public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
            bot.scheduleRandomSound(event.getGuild());
        }
    }
}
