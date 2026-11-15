package com.nonton.app.data.provider

/** Raised when content is requested before any usable provider is configured. */
class ProviderNotConfiguredException : IllegalStateException(
    "Belum ada provider aktif. Tambahkan provider di Pengaturan Provider."
)
