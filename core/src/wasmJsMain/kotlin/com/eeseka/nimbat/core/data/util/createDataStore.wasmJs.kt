package com.eeseka.nimbat.core.data.util

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.WebLocalStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer

fun createDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.create(
        storage = WebLocalStorage(serializer = PreferencesSerializer, name = DATA_STORE_FILE_NAME)
    )
}
