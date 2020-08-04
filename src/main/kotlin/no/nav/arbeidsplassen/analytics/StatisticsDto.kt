package no.nav.arbeidsplassen.analytics

interface StatisticsDto<T : StatisticsDto<T>> {

    infix fun mergeWith(other: T): T
}