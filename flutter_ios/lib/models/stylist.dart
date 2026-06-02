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
  final String? salonAddress;
  final String? businessAddress;
  final String? serviceLocationType;
  final String? specialty;
  final bool offersAtHomeService;
  final double atHomeServiceFee;
  final int maxTravelRangeKm;

  // ── Stripe Connect fields (new) ──────────────────
  /// Stripe Connect Express account ID.
  /// Set automatically by Firebase webhook after onboarding.
  final String? stripeAccountId;

  /// "active" | "pending" | null
  final String? stripeAccountStatus;
  final bool? stripeChargesEnabled;
  final bool? stripePayoutsEnabled;
  final bool? stripeOnboardingComplete;
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
    this.salonAddress,
    this.businessAddress,
    this.serviceLocationType,
    this.specialty,
    this.offersAtHomeService = false,
    this.atHomeServiceFee = 0.0,
    this.maxTravelRangeKm = 15,
    this.stripeAccountId,
    this.stripeAccountStatus,
    this.stripeChargesEnabled,
    this.stripePayoutsEnabled,
    this.stripeOnboardingComplete,
  });

  bool get hasActivePayoutAccount =>
      stripeAccountStatus == 'active' ||
      stripeOnboardingComplete == true ||
      (stripeChargesEnabled == true && stripePayoutsEnabled == true);

  String? get displayAddress {
    for (final value in [salonAddress, address, businessAddress]) {
      final trimmed = value?.trim();
      if (trimmed != null && trimmed.isNotEmpty) return trimmed;
    }
    return null;
  }

  bool get hasFixedPublicLocation {
    final loc = location;
    if (loc == null) return false;
    if (loc.latitude == 0.0 && loc.longitude == 0.0) return false;

    final fixedByType = serviceLocationType == 'fixed';
    final legacyFixedProfile =
        (serviceLocationType == null || serviceLocationType!.trim().isEmpty) &&
        !offersAtHomeService;
    return (fixedByType || legacyFixedProfile) &&
        (displayAddress?.isNotEmpty ?? false);
  }

  factory Stylist.fromFirestore(DocumentSnapshot doc) {
    final data = doc.data() as Map<String, dynamic>;
    return Stylist(
      id: doc.id,
      name: data['name'] as String? ?? '',
      profileImageUrl: data['profileImageUrl'] as String?,
      rating: (data['rating'] as num?)?.toDouble() ?? 0.0,
      isAvailable: data['isAvailable'] as bool? ?? true,
      location: data['location'] as GeoPoint?,
      services:
          (data['services'] as List<dynamic>?)
              ?.map((s) => Service.fromMap(s as Map<String, dynamic>))
              .toList() ??
          [],
      bio: data['bio'] as String?,
      address: data['address'] as String?,
      salonAddress: data['salonAddress'] as String?,
      businessAddress: data['businessAddress'] as String?,
      serviceLocationType: data['serviceLocationType'] as String?,
      specialty: data['specialty'] as String?,
      offersAtHomeService: data['offersAtHomeService'] as bool? ?? false,
      atHomeServiceFee: (data['atHomeServiceFee'] as num?)?.toDouble() ?? 0.0,
      maxTravelRangeKm: (data['maxTravelRangeKm'] as num?)?.toInt() ?? 15,
      stripeAccountId: data['stripeAccountId'] as String?,
      stripeAccountStatus: data['stripeAccountStatus'] as String?,
      stripeChargesEnabled: data['stripeChargesEnabled'] as bool?,
      stripePayoutsEnabled: data['stripePayoutsEnabled'] as bool?,
      stripeOnboardingComplete: data['stripeOnboardingComplete'] as bool?,
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
      'salonAddress': salonAddress,
      'businessAddress': businessAddress,
      'serviceLocationType': serviceLocationType,
      'specialty': specialty,
      'offersAtHomeService': offersAtHomeService,
      'atHomeServiceFee': atHomeServiceFee,
      'maxTravelRangeKm': maxTravelRangeKm,
      'stripeAccountId': stripeAccountId,
      'stripeAccountStatus': stripeAccountStatus,
      'stripeChargesEnabled': stripeChargesEnabled,
      'stripePayoutsEnabled': stripePayoutsEnabled,
      'stripeOnboardingComplete': stripeOnboardingComplete,
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
    String? salonAddress,
    String? businessAddress,
    String? serviceLocationType,
    String? specialty,
    bool? offersAtHomeService,
    double? atHomeServiceFee,
    int? maxTravelRangeKm,
    String? stripeAccountId,
    String? stripeAccountStatus,
    bool? stripeChargesEnabled,
    bool? stripePayoutsEnabled,
    bool? stripeOnboardingComplete,
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
      salonAddress: salonAddress ?? this.salonAddress,
      businessAddress: businessAddress ?? this.businessAddress,
      serviceLocationType: serviceLocationType ?? this.serviceLocationType,
      specialty: specialty ?? this.specialty,
      offersAtHomeService: offersAtHomeService ?? this.offersAtHomeService,
      atHomeServiceFee: atHomeServiceFee ?? this.atHomeServiceFee,
      maxTravelRangeKm: maxTravelRangeKm ?? this.maxTravelRangeKm,
      stripeAccountId: stripeAccountId ?? this.stripeAccountId,
      stripeAccountStatus: stripeAccountStatus ?? this.stripeAccountStatus,
      stripeChargesEnabled: stripeChargesEnabled ?? this.stripeChargesEnabled,
      stripePayoutsEnabled: stripePayoutsEnabled ?? this.stripePayoutsEnabled,
      stripeOnboardingComplete:
          stripeOnboardingComplete ?? this.stripeOnboardingComplete,
    );
  }
}

class Service {
  final String id;
  final String name;
  final double price;
  final int durationMinutes;
  final String? description;
  final bool isBundle;
  final bool isAddOn;

  const Service({
    this.id = '',
    required this.name,
    required this.price,
    this.durationMinutes = 60,
    this.description,
    this.isBundle = false,
    this.isAddOn = false,
  });

  factory Service.fromMap(Map<String, dynamic> map) {
    return Service(
      id: map['id'] as String? ?? '',
      name: map['name'] as String? ?? '',
      price: (map['price'] as num?)?.toDouble() ?? 0.0,
      durationMinutes: map['durationMinutes'] as int? ?? 60,
      description: map['description'] as String?,
      isBundle: map['isBundle'] as bool? ?? map['bundle'] as bool? ?? false,
      isAddOn:
          map['isAddOn'] as bool? ??
          map['addOn'] as bool? ??
          _looksLikeAddOn(map['name'] as String? ?? ''),
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'price': price,
      'durationMinutes': durationMinutes,
      'description': description,
      'isBundle': isBundle,
      'bundle': isBundle,
      'isAddOn': isAddOn,
      'addOn': isAddOn,
    };
  }

  static bool _looksLikeAddOn(String name) {
    final lower = name.toLowerCase();
    return lower.contains('add-on') || lower.contains('addon');
  }
}
