package com.management.models

import jakarta.persistence.Entity
import jakarta.persistence.*
import org.hibernate.envers.Audited
import java.time.LocalDateTime

@Entity
@Audited
@Table(
    name = "user_team",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "team_id"])]
)
class UserTeam (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    var team: Team? = null,

    @Column(nullable = false)
    var joinedAt: LocalDateTime = LocalDateTime.now(),
)