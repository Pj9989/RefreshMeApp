/// RefreshMe — Stylist Model
/// Place this file at: lib/models/stylist.dart
///
/// Merge the stripeAccountId + stripeAccountStatus fields into your
/// existing Stylist model/class. Full example shown below.

import 'package:cloud_firestore/cloud_firestore.dart';

class Stylist {
  final String id;
  final String name;
  final String? profileImageUrl;
  final double rating;
  final bool isAvailable;
  final GeoPoint? location;
  final List<Service> services;
  final String? bio;
  final String? address;
  final String? specialty;

  // ── Stripe Connect fields (new) ──────────────────
  /// Stripe Connect Express account ID.
  /// Set automatically by Firebase webhook after onboarding.
  final String? stripeAccountId;

  /// "active" | "pending" | null
  final String? stripeAccountStatus;
  // ─────────────────────────────────────────────────

  const Stylist({
    required this.id,
    required this.name,
    this.profileImageUrl,
    this.rating = 0.0,
    this.isAvailable = true,
    this.location,
    this.services = const [],
    this.bio,
    this.address,
    this.specialty,
    this.stripeAccountId,
    this.stripeAccountStatus,
  });

  bool get hasActivePayoutAccount => stripeAccountStatus == 'active';

  factory Stylist.fromFirestore(DocumentSnapshot doc) {
    final data = doc.data() as Map<String, dynamic>;
    return Stylist(
      id: doc.id,
      name: data['name'] as String? ?? '',
      profileImageUrl: data['profileImageUrl'] as String?,
      rating: (data['rating'] as num?)?.toDouble() ?? 0.0,
      isAvailable: data['isAvailable'] as bool? ?? true,
      location: data['location'] as GeoPoint?,
      services: (data['services'] as List<dynamic>?)
              ?.map((s) => Service.fromMap(s as Map<String, dynamic>))
              .toList() ??
          [],
      bio: data['bio'] as String?,
      address: data['address'] as String?,
      specialty: data['specialty'] as String?,
      stripeAccountId: data['stripeAccountId'] as String?,
      stripeAccountStatus: data['stripeAccountStatus'] as String?,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'name': name,
      'profileImageUrl': profileImageUrl,
      'rating': rating,
      'isAvailable': isAvailable,
      'location': location,
      'services': services.map((s) => s.toMap()).toList(),
      'bio': bio,
      'address': address,
      'specialty': specialty,
      'stripeAccountId': stripeAccountId,
      'stripeAccountStatus': stripeAccountStatus,
    };
  }

  Stylist copyWith({
    String? id,
    String? name,
    String? profileImageUrl,
    double? rating,
    bool? isAvailable,
    GeoPoint? location,
    List<Service>? services,
    String? bio,
    String? address,
    String? specialty,
    String? stripeAccountId,
    String? stripeAccountStatus,
  }) {
    return Stylist(
      id: id ?? this.id,
      name: name ?? this.name,
      profileImageUrl: profileImageUrl ?? this.profileImageUrl,
      rating: rating ?? this.rating,
      isAvailable: isAvailable ?? this.isAvailable,
      location: location ?? this.location,
      services: services ?? this.services,
      bio: bio ?? this.bio,
      address: address ?? this.address,
      specialty: specialty ?? this.specialty,
      stripeAccountId: stripeAccountId ?? this.stripeAccountId,
      stripeAccountStatus: stripeAccountStatus ?? this.stripeAccountStatus,
    );
  }
}

class Service {
  final String name;
  final double price;
  final int durationMinutes;
  final String? description;

  const Service({
    required this.name,
    required this.price,
    this.durationMinutes = 60,
    this.description,
  });

  factory Service.fromMap(Map<String, dynamic> map) {
    return Service(
      name: map['name'] as String? ?? '',
      price: (map['price'] as num?)?.toDouble() ?? 0.0,
      durationMinutes: map['durationMinutes'] as int? ?? 60,
      description: map['description'] as String?,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'name': name,
      'price': price,
      'durationMinutes': durationMinutes,
      'description': description,
    };
  }
}
