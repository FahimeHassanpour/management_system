package com.management.models

import jakarta.persistence.*
import org.hibernate.envers.Audited

@Entity
@Audited
@Table(name = "teams")
class Team(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var name: String = "",

    @Column(length = 500)
    var description: String = "",

    @OneToMany(mappedBy = "team", cascade = [CascadeType.ALL], orphanRemoval = true)
    var userTeams: MutableSet<UserTeam> = mutableSetOf(),
)
