/*  _____ _
 * |_   _| |_  _ _ ___ ___ _ __  __ _
 *   | | | ' \| '_/ -_) -_) '  \/ _` |_
 *   |_| |_||_|_| \___\___|_|_|_\__,_(_)
 *
 * Threema for Android
 * Copyright (c) 2023-2024 Threema GmbH
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
import ch.threema.app.utils.MessageUtil
import ch.threema.base.utils.LoggingUtil
import ch.threema.domain.models.MessageId
import ch.threema.domain.protocol.csp.messages.GroupDeliveryReceiptMessage
import ch.threema.protobuf.Common
import ch.threema.protobuf.d2d.MdD2D
import ch.threema.storage.models.GroupMessageModel
import ch.threema.storage.models.MessageState
import java.util.Date

private val logger = LoggingUtil.getThreemaLogger("ReflectedOutgoingGroupDeliveryReceiptTask")

internal class ReflectedOutgoingGroupDeliveryReceiptTask(
    message: MdD2D.OutgoingMessage,
    serviceManager: ServiceManager,
) : ReflectedOutgoingContactMessageTask(
    message,
    Common.CspE2eMessageType.GROUP_DELIVERY_RECEIPT,
    serviceManager
) {
    private val messageService by lazy { serviceManager.messageService }
    private val myIdentity by lazy { serviceManager.identityStore.identity }

    private val groupDeliveryReceiptMessage by lazy { GroupDeliveryReceiptMessage.fromReflected(message) }

    override val shouldBumpLastUpdate: Boolean = false

    override val storeNonces: Boolean
        get() = groupDeliveryReceiptMessage.protectAgainstReplay()

    override fun processOutgoingMessage() {
        logger.info("Processing message {}: reflected outgoing group delivery receipt", message.messageId)

        val messageState: MessageState? = MessageUtil.receiptTypeToMessageState(groupDeliveryReceiptMessage.receiptType)
        if (messageState == null || !MessageUtil.isReaction(messageState)) {
            logger.warn(
                "Message {} error: unknown or unsupported delivery receipt type: {}",
                groupDeliveryReceiptMessage.messageId,
                groupDeliveryReceiptMessage.receiptType
            )
            return
        }

        for (receiptMessageId: MessageId in groupDeliveryReceiptMessage.receiptMessageIds) {
            logger.info(
                "Processing message {}: group delivery receipt for {} (state = {})",
                message.messageId,
                receiptMessageId,
                messageState
            )
            val groupMessageModel: GroupMessageModel? = messageService.getGroupMessageModel(
                receiptMessageId,
                groupDeliveryReceiptMessage.groupCreator,
                groupDeliveryReceiptMessage.apiGroupId
            )
            if (groupMessageModel == null) {
                logger.warn("Group message model ({}) for reflected outgoing group delivery receipt is null", receiptMessageId)
                continue
            }
            messageService.addMessageReaction(
                groupMessageModel,
                messageState,
                myIdentity, // the identity that reacted (this is us => reflected outgoing message)
                Date(message.createdAt)
            )
        }
    }
}
