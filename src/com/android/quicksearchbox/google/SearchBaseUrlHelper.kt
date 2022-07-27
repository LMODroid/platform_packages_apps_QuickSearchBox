/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.quicksearchbox.google

import com.android.quicksearchbox.R

/**
 * Helper to build the base URL for all search requests.
 */
class SearchBaseUrlHelper(
    context: Context, helper: HttpHelper,
    searchSettings: SearchSettings, prefs: SharedPreferences
) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val mHttpHelper: HttpHelper
    private val mContext: Context
    private val mSearchSettings: SearchSettings

    /**
     * Update the base search url, either:
     * (a) it has never been set (first run)
     * (b) it has expired
     * (c) if the caller forces an update by setting the "force" parameter.
     *
     * @param force if true, then the URL is reset whether or not it has
     * expired.
     */
    fun maybeUpdateBaseUrlSetting(force: Boolean) {
        val lastUpdateTime: Long = mSearchSettings.getSearchBaseDomainApplyTime()
        val currentTime: Long = System.currentTimeMillis()
        if (force || lastUpdateTime == -1L || currentTime - lastUpdateTime >= SearchBaseUrlHelper.Companion.SEARCH_BASE_URL_EXPIRY_MS) {
            if (mSearchSettings.shouldUseGoogleCom()) {
                setSearchBaseDomain(defaultBaseDomain)
            } else {
                checkSearchDomain()
            }
        }
    }

    /**
     * @return the base url for searches.
     */
    val searchBaseUrl: String
        get() = mContext.getResources().getString(
            R.string.google_search_base_pattern,
            searchDomain, GoogleSearch.getLanguage(Locale.getDefault())
        )// This is required to deal with the case wherein getSearchDomain
    // is called before checkSearchDomain returns a valid URL. This will
    // happen *only* on the first run of the app when the "use google.com"
    // option is unchecked. In other cases, the previously set domain (or
    // the default) will be returned.
    //
    // We have no choice in this case but to use the default search domain.
    /**
     * @return the search domain. This is of the form "google.co.xx" or "google.com",
     * used by UI code.
     */
    val searchDomain: String?
        get() {
            var domain: String = mSearchSettings.getSearchBaseDomain()
            if (domain == null) {
                if (SearchBaseUrlHelper.Companion.DBG) {
                    Log.w(
                        SearchBaseUrlHelper.Companion.TAG,
                        "Search base domain was null, last apply time=" +
                                mSearchSettings.getSearchBaseDomainApplyTime()
                    )
                }

                // This is required to deal with the case wherein getSearchDomain
                // is called before checkSearchDomain returns a valid URL. This will
                // happen *only* on the first run of the app when the "use google.com"
                // option is unchecked. In other cases, the previously set domain (or
                // the default) will be returned.
                //
                // We have no choice in this case but to use the default search domain.
                domain = defaultBaseDomain
            }
            if (domain.startsWith(".")) {
                if (SearchBaseUrlHelper.Companion.DBG) Log.d(
                    SearchBaseUrlHelper.Companion.TAG,
                    "Prepending www to $domain"
                )
                domain = "www$domain"
            }
            return domain
        }

    /**
     * Issue a request to google.com/searchdomaincheck to retrieve the base
     * URL for search requests.
     */
    private fun checkSearchDomain() {
        val request = GetRequest(SearchBaseUrlHelper.Companion.DOMAIN_CHECK_URL)
        object : AsyncTask<Void?, Void?, Void?>() {
            @Override
            protected fun doInBackground(vararg params: Void?): Void? {
                if (SearchBaseUrlHelper.Companion.DBG) Log.d(
                    SearchBaseUrlHelper.Companion.TAG,
                    "Starting request to /searchdomaincheck"
                )
                var domain: String
                try {
                    domain = mHttpHelper.get(request)
                } catch (e: Exception) {
                    if (SearchBaseUrlHelper.Companion.DBG) Log.d(
                        SearchBaseUrlHelper.Companion.TAG,
                        "Request to /searchdomaincheck failed : $e"
                    )
                    // Swallow any exceptions thrown by the HTTP helper, in
                    // this rare case, we just use the default URL.
                    domain = defaultBaseDomain
                    return null
                }
                if (SearchBaseUrlHelper.Companion.DBG) Log.d(
                    SearchBaseUrlHelper.Companion.TAG,
                    "Request to /searchdomaincheck succeeded"
                )
                setSearchBaseDomain(domain)
                return null
            }
        }.execute()
    }

    private val defaultBaseDomain: String
        private get() = mContext.getResources().getString(R.string.default_search_domain)

    private fun setSearchBaseDomain(domain: String) {
        if (SearchBaseUrlHelper.Companion.DBG) Log.d(
            SearchBaseUrlHelper.Companion.TAG,
            "Setting search domain to : $domain"
        )
        mSearchSettings.setSearchBaseDomain(domain)
    }

    @Override
    fun onSharedPreferenceChanged(pref: SharedPreferences?, key: String) {
        // Listen for changes only to the SEARCH_BASE_URL preference.
        if (SearchBaseUrlHelper.Companion.DBG) Log.d(
            SearchBaseUrlHelper.Companion.TAG,
            "Handling changed preference : $key"
        )
        if (SearchSettingsImpl.USE_GOOGLE_COM_PREF.equals(key)) {
            maybeUpdateBaseUrlSetting(true)
        }
    }

    companion object {
        private const val DBG = false
        private const val TAG = "QSB.SearchBaseUrlHelper"
        private const val DOMAIN_CHECK_URL =
            "https://www.google.com/searchdomaincheck?format=domain"
        private const val SEARCH_BASE_URL_EXPIRY_MS = 24 * 3600 * 1000L
    }

    /**
     * Note that this constructor will spawn a thread to issue a HTTP
     * request if shouldUseGoogleCom is false.
     */
    init {
        mHttpHelper = helper
        mContext = context
        mSearchSettings = searchSettings

        // Note: This earlier used an inner class, but that causes issues
        // because SharedPreferencesImpl uses a WeakHashMap< > and the listener
        // will be GC'ed unless we keep a reference to it here.
        prefs.registerOnSharedPreferenceChangeListener(this)
        maybeUpdateBaseUrlSetting(false)
    }
}