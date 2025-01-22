/*  _____ _
 * |_   _| |_  _ _ ___ ___ _ __  __ _
 *   | | | ' \| '_/ -_) -_) '  \/ _` |_
 *   |_| |_||_|_| \___\___|_|_|_\__,_(_)
 *
 * Threema for Android
 * Copyright (c) 2014-2024 Threema GmbH
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

package ch.threema.app.activities

import ch.threema.app.R
import ch.threema.app.ThreemaApplication

class BlockedIdentitiesActivity : IdentityListActivity() {
    private val identityList: IdentityList? by lazy {
        val blockedIdentitiesService =
            ThreemaApplication.getServiceManager()?.blockedIdentitiesService ?: return@lazy null

        object : IdentityList {
            override fun getAll(): Set<String> {
                return blockedIdentitiesService.getAllBlockedIdentities()
            }

            override fun addIdentity(identity: String) {
                blockedIdentitiesService.blockIdentity(identity)
            }

            override fun removeIdentity(identity: String) {
                blockedIdentitiesService.unblockIdentity(identity)
            }
        }
    }

    override fun getIdentityListHandle(): IdentityList? {
        return identityList
    }

    override fun getBlankListText(): String {
        return this.getString(R.string.prefs_sum_blocked_contacts)
    }

    override fun getTitleText(): String {
        return this.getString(R.string.prefs_title_blocked_contacts)
    }
}
