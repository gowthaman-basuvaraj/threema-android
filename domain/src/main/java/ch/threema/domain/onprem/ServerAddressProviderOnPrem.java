/*  _____ _
 * |_   _| |_  _ _ ___ ___ _ __  __ _
 *   | | | ' \| '_/ -_) -_) '  \/ _` |_
 *   |_| |_||_|_| \___\___|_|_|_\__,_(_)
 *
 * Threema for Android
 * Copyright (c) 2021-2024 Threema GmbH
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

package ch.threema.domain.onprem;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.threema.base.ThreemaException;
import ch.threema.base.utils.Utils;
import ch.threema.domain.protocol.ServerAddressProvider;
import ch.threema.domain.protocol.connection.d2m.MultiDevicePropertyProvider;

public class ServerAddressProviderOnPrem implements ServerAddressProvider {

    public interface FetcherProvider {
        OnPremConfigFetcher getFetcher() throws ThreemaException;
    }

    private final FetcherProvider fetcherProvider;

    public ServerAddressProviderOnPrem(FetcherProvider fetcherProvider) {
        this.fetcherProvider = fetcherProvider;
    }

    @Override
    public String getChatServerNamePrefix(boolean ipv6) throws ThreemaException {
        return "";
    }

    @Override
    public String getChatServerNameSuffix(boolean ipv6) throws ThreemaException {
        return getOnPremConfigFetcher().fetch().getChatConfig().getHostname();
    }

    @Override
    public int[] getChatServerPorts() throws ThreemaException {
        return getOnPremConfigFetcher().fetch().getChatConfig().getPorts();
    }

    @Override
    public boolean getChatServerUseServerGroups() {
        return false;
    }

    @Override
    public byte[] getChatServerPublicKey() throws ThreemaException {
        return getOnPremConfigFetcher().fetch().getChatConfig().getPublicKey();
    }

    @Override
    public byte[] getChatServerPublicKeyAlt() throws ThreemaException {
        // No alternate public key for OnPrem, as it can easily be switched in OPPF
        return getOnPremConfigFetcher().fetch().getChatConfig().getPublicKey();
    }

    @Override
    public String getDirectoryServerUrl(boolean ipv6) throws ThreemaException {
        return getOnPremConfigFetcher().fetch().getDirectoryConfig().getUrl();
    }

    @Override
    public String getWorkServerUrl(boolean ipv6) throws ThreemaException {
        return getOnPremConfigFetcher().fetch().getWorkConfig().getUrl();
    }

    // TODO(ANDR-3375): Return correct base url of mirror server
    @NonNull
    @Override
    public String getBlobBaseUrlMirrorServer(@NonNull MultiDevicePropertyProvider multiDevicePropertyProvider) throws ThreemaException {
        throw new ThreemaException("Not yet implemented.");
    }

    @NonNull
    @Override
    public String getBlobServerDownloadUrl(boolean useIpV6, @NonNull byte[] blobId) throws ThreemaException {
        final @Nullable String blobIdHexString = Utils.byteArrayToHexString(blobId);
        if (blobIdHexString == null || blobIdHexString.isBlank()) {
            throw new ThreemaException("Argument blobId is not in correct form");
        }
        return getOnPremConfigFetcher()
            .fetch()
            .getBlobConfig()
            .getDownloadUrl()
            .replace(OnPremConfigBlob.PLACEHOLDER_BLOB_ID, blobIdHexString);
    }

    @NonNull
    @Override
    public String getBlobServerUploadUrl(boolean useIpV6) throws ThreemaException {
        return getOnPremConfigFetcher().fetch().getBlobConfig().getUploadUrl();
    }

    @NonNull
    @Override
    public String getBlobServerDoneUrl(boolean useIpV6, @NonNull byte[] blobId) throws ThreemaException {
        final @Nullable String blobIdHexString = Utils.byteArrayToHexString(blobId);
        if (blobIdHexString == null || blobIdHexString.isBlank()) {
            throw new ThreemaException("Argument blobId is not in correct form");
        }
        return getOnPremConfigFetcher()
            .fetch()
            .getBlobConfig()
            .getDoneUrl()
            .replace(OnPremConfigBlob.PLACEHOLDER_BLOB_ID, blobIdHexString);
    }

    // TODO(ANDR-3375): Return correct url of mirror server
    @NonNull
    @Override
    public String getBlobMirrorServerDownloadUrl(
        @NonNull MultiDevicePropertyProvider multiDevicePropertyProvider,
        @NonNull byte[] blobId
    ) throws ThreemaException {
        return getBlobServerDownloadUrl(false, blobId);
    }

    // TODO(ANDR-3375): Return correct url of mirror server
    @NonNull
    @Override
    public String getBlobMirrorServerUploadUrl(
        @NonNull MultiDevicePropertyProvider multiDevicePropertyProvider
    ) throws ThreemaException {
        return getBlobServerUploadUrl(false);
    }

    // TODO(ANDR-3375): Return correct url of mirror server
    @NonNull
    @Override
    public String getBlobMirrorServerDoneUrl(
        @NonNull MultiDevicePropertyProvider multiDevicePropertyProvider,
        @NonNull byte[] blobId
    ) throws ThreemaException {
        return getBlobServerDoneUrl(false, blobId);
    }

    @Override
    public String getAvatarServerUrl(boolean ipv6) throws ThreemaException {
        return getOnPremConfigFetcher().fetch().getAvatarConfig().getUrl();
    }

    @Override
    public String getSafeServerUrl(boolean ipv6) throws ThreemaException {
        return getOnPremConfigFetcher().fetch().getSafeConfig().getUrl();
    }

    @Override
    @Nullable
    public String getWebServerUrl() throws ThreemaException {
        OnPremConfigWeb onPremConfigWeb = getOnPremConfigFetcher().fetch().getWebConfig();

        if (onPremConfigWeb != null) {
            return onPremConfigWeb.getUrl();
        }
        throw new ThreemaException("Unable to fetch Threema Web server url");
    }

    @Override
    public String getWebOverrideSaltyRtcHost() throws ThreemaException {
        OnPremConfigWeb onPremConfigWeb = getOnPremConfigFetcher().fetch().getWebConfig();

        if (onPremConfigWeb != null) {
            return onPremConfigWeb.getOverrideSaltyRtcHost();
        }
        return null;
    }

    @Override
    public int getWebOverrideSaltyRtcPort() throws ThreemaException {
        OnPremConfigWeb onPremConfigWeb = getOnPremConfigFetcher().fetch().getWebConfig();

        if (onPremConfigWeb != null) {
            return onPremConfigWeb.getOverrideSaltyRtcPort();
        }
        return 0;
    }

    @Override
    public byte[] getThreemaPushPublicKey() throws ThreemaException {
        // TODO(ONPREM-164): Allow to configure for OnPrem
        return null;
    }

    @NonNull
    @Override
    public String getMediatorUrl() throws ThreemaException {
        OnPremConfigMediator onPremConfigMediator = getOnPremConfigFetcher().fetch().getMediatorConfig();

        if (onPremConfigMediator == null) {
            throw new ThreemaException("No mediator config available");
        }
        return Objects.requireNonNull(onPremConfigMediator.getUrl());
    }

    @NonNull
    @Override
    public String getAppRatingUrl() throws ThreemaException {
        throw new ThreemaException("App rating is not supported in onprem");
    }

    private OnPremConfigFetcher getOnPremConfigFetcher() throws ThreemaException {
        return fetcherProvider.getFetcher();
    }
}
