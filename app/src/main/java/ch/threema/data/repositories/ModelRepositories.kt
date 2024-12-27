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

package ch.threema.data.repositories

import ch.threema.base.utils.LoggingUtil
import ch.threema.data.ModelCache
import ch.threema.data.storage.EditHistoryDaoImpl
import ch.threema.data.storage.SqliteDatabaseBackend
import ch.threema.storage.DatabaseServiceNew

class ModelRepositories(databaseService: DatabaseServiceNew) {
    private val logger = LoggingUtil.getThreemaLogger("data.ModelRepositories")

    private val cache = ModelCache()
    private val databaseBackend = SqliteDatabaseBackend(databaseService)
    private val editHistoryDao = EditHistoryDaoImpl(databaseService)

    val contacts = ContactModelRepository(cache.contacts, databaseBackend)
    val groups = GroupModelRepository(cache.groups, databaseBackend)
    val editHistory = EditHistoryRepository(cache.editHistory, editHistoryDao)

    init {
        logger.debug("Created")
    }
}
