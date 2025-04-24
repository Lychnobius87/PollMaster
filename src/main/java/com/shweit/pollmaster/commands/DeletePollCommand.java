package com.shweit.pollmaster.commands;

import com.shweit.pollmaster.utils.ConnectionManager;
import com.shweit.pollmaster.utils.LangUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class DeletePollCommand implements CommandExecutor, TabExecutor {


    public boolean deletePoll(final Player whoClicked, final int pollId) {

        // Connect to the database
        try (Connection connection = new ConnectionManager().getConnection()) {
            // Check if the player is the creator of the poll
            String checkCreatorQuery = "SELECT uuid FROM polls WHERE id = ?";
            try (PreparedStatement checkCreatorStmt = connection.prepareStatement(checkCreatorQuery)) {
                checkCreatorStmt.setInt(1, pollId);
                try (ResultSet resultSet = checkCreatorStmt.executeQuery()) {
                    if (resultSet.next()) {
                        String creatorUUID = resultSet.getString("uuid");
                        if (!creatorUUID.equals(whoClicked.getUniqueId().toString())) {
                            whoClicked.sendMessage(ChatColor.RED + LangUtil.getTranslation("delete_poll_not_creator"));
                            return false;
                        }
                    } else {
                        whoClicked.sendMessage(ChatColor.RED + LangUtil.getTranslation("poll_not_found"));
                        return false;
                    }
                }
            }


            // Prepare the SQL statements to delete the poll and associated votes
            String deletePollQuery = "DELETE FROM polls WHERE id = ?";
            String deleteVotesQuery = "DELETE FROM votes WHERE poll_id = ?";

            // Delete the poll itself
            try (PreparedStatement deletePollStmt = connection.prepareStatement(deletePollQuery)) {
                deletePollStmt.setInt(1, pollId);
                int rowsAffectedPolls = deletePollStmt.executeUpdate();

                // Check if the poll was successfully deleted
                if (rowsAffectedPolls > 0) {
                    // Delete associated votes
                    try (PreparedStatement deleteVotesStmt = connection.prepareStatement(deleteVotesQuery)) {
                        deleteVotesStmt.setInt(1, pollId);
                        deleteVotesStmt.executeUpdate(); // Delete all votes associated with the poll
                    }

                    whoClicked.sendMessage(ChatColor.GREEN + LangUtil.getTranslation("poll_deleted"));
                    return true;
                } else {
                    whoClicked.sendMessage(ChatColor.RED + LangUtil.getTranslation("poll_not_found"));
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            whoClicked.sendMessage(ChatColor.RED + LangUtil.getTranslation("poll_deleted_error"));
        }

        return false;
    }


    @Override
    public boolean onCommand(final CommandSender commandSender, final Command command, final String s, final String[] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(LangUtil.getTranslation("command_no_player"));
            return true;
        }

        if (!player.hasPermission("pollmaster.delete")) {
            player.sendMessage(ChatColor.RED + LangUtil.getTranslation("command_no_permission"));
            return true;
        }

        deletePoll(player, Integer.parseInt(args[0]));
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender commandSender, final Command command, final String s, final String[] args) {
        if (args.length == 1) {
            return getPlayerPolls((Player) commandSender);
        }

        return new ArrayList<>();
    }

    private ArrayList<String> getPlayerPolls(final Player player) {
        ArrayList<String> pollData = new ArrayList<>();
        String query = "SELECT id FROM polls WHERE isOpen = 1 AND uuid = ?";

        // Create connection and statement using try-with-resources
        try (Connection connection = new ConnectionManager().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            // Set the player's UUID as a parameter in the prepared statement
            statement.setString(1, player.getUniqueId().toString());

            // Execute the query and process the results
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    pollData.add(String.valueOf(results.getInt("id")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return pollData;
    }

}
