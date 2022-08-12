package net.irisshaders.lilybot.extensions.config

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalRole
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.GuildMessageChannel
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.ConfigData
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.botHasChannelPerms

/**
 * The class for configuring LilyBot in your guilds.
 *
 * @since 2.1.0
 */
class Config : Extension() {

	override val name = "config"

	override suspend fun setup() {
		/**
		 * The parent command for the commands to handle configuration
		 * @author NoComment1105, tempest15
		 * @since 2.1.0
		 */
		ephemeralSlashCommand {
			name = "config"
			description = "Configuration set up commands!"

			ephemeralSubCommand(::Config) {
				name = "set"
				description = "Set the config"

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
					requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
					botHasChannelPerms(
						Permissions(Permission.SendMessages, Permission.EmbedLinks)
					)
				}

				action {
					val actionLogChannel = guild!!.getChannelOf<GuildMessageChannel>(arguments.modActionLog.id)

					// If an action log ID doesn't exist, set the config
					// Otherwise, inform the user their config is already set
					if (DatabaseHelper.getConfig(guild!!.id)?.modActionLog == null) {
						val newConfig = ConfigData(
							guild!!.id,
							arguments.moderatorPing.id,
							arguments.modActionLog.id,
							arguments.messageLogs.id,
							arguments.joinChannel.id,
							arguments.supportChannel?.id,
							arguments.supportTeam?.id,
						)

						DatabaseHelper.setConfig(newConfig)

						respond { content = "Config Set for Guild ID: ${guild!!.id}!" }
					} else {
						respond { content = "**Error:** There is already a configuration set for this guild!" }
						return@action
					}

					actionLogChannel.createEmbed {
						title = "Configuration set!"
						description = "A guild manager has set a config for this guild!"
						color = DISCORD_BLACK
						timestamp = Clock.System.now()
						field {
							name = "Set values:"
							value = """
								Moderators Ping = ${arguments.moderatorPing.mention}
								Mod Action Log = ${arguments.modActionLog.mention}
								Message Logs = ${arguments.messageLogs.mention}
								Join Channel = ${arguments.joinChannel.mention}
								Support Team = ${arguments.supportTeam?.mention ?: "null"}
								Support Channel = ${arguments.supportChannel?.mention ?: "null"}
							""".trimIndent()
						}
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
					}
				}
			}

			ephemeralSubCommand {
				name = "clear"
				description = "Clear the config!"

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
					requireBotPermissions(Permission.SendMessages, Permission.EmbedLinks)
					botHasChannelPerms(
						Permissions(Permission.SendMessages, Permission.EmbedLinks)
					)
				}

				action {
					// If an action log ID resists, inform the user their config isn't set.
					// Otherwise, clear the config.
					if (DatabaseHelper.getConfig(guild!!.id)?.modActionLog == null) {
						respond { content = "**Error:** There is no configuration set for this guild!" }
						return@action // Return to avoid the database trying to delete things that don't exist
					} else {
						// Log the config being cleared to the action log
						val actionLogId = DatabaseHelper.getConfig(guild!!.id)?.modActionLog
						val actionLogChannel = guild!!.getChannelOf<GuildMessageChannel>(actionLogId!!)

						respond {
							content = "Config cleared for Guild ID: ${guild!!.id}!"
						}

						actionLogChannel.createEmbed {
							title = "Configuration cleared!"
							description = "A Guild Manager has cleared the configuration for this guild!"
							footer {
								text = user.asUser().tag
								icon = user.asUser().avatar?.url
							}
							color = DISCORD_BLACK
						}

						// Clear the config (do this last)
						DatabaseHelper.clearConfig(guild!!.id)
					}
				}
			}
		}
	}

	inner class Config : Arguments() {
		/** The role for pinging moderators. */
		val moderatorPing by role {
			name = "moderator-role"
			description = "Your Moderator role"
		}

		/** The channel for sending action logging to. */
		val modActionLog by channel {
			name = "mod-action-log"
			description = "Your Mod Action Log channel"
		}

		/** The channel for sending message logs to. */
		val messageLogs by channel {
			name = "message-logs"
			description = "Your Message Logs Channel"
		}

		/** The channel for sending join logs to. */
		val joinChannel by channel {
			name = "join-channel"
			description = "Your Join Logs Channel"
		}

		/** The role for the support team. */
		val supportTeam by optionalRole {
			name = "support-team-role"
			description = "Your Support Team role"
		}

		/** The channel for creating support threads in. */
		val supportChannel by optionalChannel {
			name = "support-channel"
			description = "Your Support Channel"
		}
	}
}
