/*  _____ _
 * |_   _| |_  _ _ ___ ___ _ __  __ _
 *   | | | ' \| '_/ -_) -_) '  \/ _` |_
 *   |_| |_||_|_| \___\___|_|_|_\__,_(_)
 *
 * Threema for Android
 * Copyright (c) 2024 Threema GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.threema.app.processors.reflectedoutgoingmessage

import ch.threema.app.managers.ServiceManager
import ch.threema.domain.protocol.csp.messages.ballot.GroupPollSetupMessage
import ch.threema.protobuf.Common
import ch.threema.protobuf.d2d.MdD2D

internal class ReflectedOutgoingGroupPollSetupMessageTask(
    message: MdD2D.OutgoingMessage,
    serviceManager: ServiceManager,
) : ReflectedOutgoingGroupMessageTask(
    message,
    Common.CspE2eMessageType.GROUP_POLL_SETUP,
    serviceManager,
) {

    private val myIdentity by lazy { serviceManager.identityStore.identity }

    private val groupPollSetupMessage: GroupPollSetupMessage by lazy {
        GroupPollSetupMessage.fromReflected(message, myIdentity)
    }

    private val ballotService by lazy { serviceManager.ballotService }

    override val shouldBumpLastUpdate: Boolean
        get() = groupPollSetupMessage.bumpLastUpdate()
    override val storeNonces: Boolean
        get() = groupPollSetupMessage.protectAgainstReplay()

    override fun processOutgoingMessage() {
        handleReflectedOutgoingPoll(
            groupPollSetupMessage,
            groupPollSetupMessage.messageId,
            messageReceiver,
            ballotService
        )
    }
}
