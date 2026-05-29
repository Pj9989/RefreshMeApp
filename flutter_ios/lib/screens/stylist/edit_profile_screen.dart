import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:cloud_functions/cloud_functions.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:url_launcher/url_launcher.dart';

class EditProfileScreen extends StatefulWidget {
  final bool isStylist;

  const EditProfileScreen({Key? key, required this.isStylist})
    : super(key: key);

  @override
  _EditProfileScreenState createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends State<EditProfileScreen> {
  final _auth = FirebaseAuth.instance;
  final _firestore = FirebaseFirestore.instance;

  String? name;
  String? bio;
  String? instagram;
  String? tiktok;
  String? profileImageUrl;
  String? licenseImageUrl;
  String? verificationStatus = "UNVERIFIED";
  bool isVerified = false;
  String serviceLocationType = "mobile";
  int serviceRadius = 9;
  double atHomeServiceFee = 20.0;
  bool _isLoadingProfile = true;
  String? _profileLoadError;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    final uid = _auth.currentUser?.uid;
    if (uid == null) {
      if (!mounted) return;
      setState(() {
        _isLoadingProfile = false;
        _profileLoadError = 'Please sign in to edit your stylist profile.';
      });
      return;
    }

    try {
      final doc = await _firestore.collection('stylists').doc(uid).get();
      if (doc.exists) {
        final data = doc.data()!;
        if (!mounted) return;
        setState(() {
          name = data['name'];
          bio = data['bio'];
          profileImageUrl = data['profileImageUrl'] ?? data['imageUrl'];
          licenseImageUrl = data['licenseImageUrl'];
          verificationStatus = data['verificationStatus'] ?? "UNVERIFIED";
          isVerified = data['verified'] == true || data['isVerified'] == true;

          final social = data['socialLinks'] as Map<String, dynamic>?;
          if (social != null) {
            instagram = social['instagram'];
            tiktok = social['tiktok'];
          }
          serviceLocationType = data['serviceLocationType'] ?? "fixed";
          serviceRadius = (data['maxTravelRangeKm'] ?? 15) ~/ 1.60934;
          atHomeServiceFee =
              (data['atHomeServiceFee'] as num?)?.toDouble() ?? 20.0;
          _isLoadingProfile = false;
          _profileLoadError = null;
        });
      } else {
        final starterName = await _createStarterStylistProfile(uid);

        if (!mounted) return;
        setState(() {
          name = starterName;
          serviceLocationType = 'mobile';
          serviceRadius = 9;
          atHomeServiceFee = 20.0;
          _isLoadingProfile = false;
          _profileLoadError = null;
        });
      }
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _isLoadingProfile = false;
        _profileLoadError =
            "We couldn't load your stylist profile. Please check your connection and try again.";
      });
    }
  }

  Future<String> _createStarterStylistProfile(String uid) async {
    final user = _auth.currentUser;
    final starterName = user?.displayName?.trim().isNotEmpty == true
        ? user!.displayName!.trim()
        : 'Stylist';

    await _firestore.collection('stylists').doc(uid).set({
      'name': starterName,
      'email': user?.email ?? '',
      'role': 'STYLIST',
      'online': false,
      'availableNow': false,
      'available': false,
      'subscriptionActive': false,
      'services': <Map<String, dynamic>>[],
      'categories': ['hair'],
      'servesGender': ['Men', 'Women', 'Non-binary'],
      'serviceLocationType': 'mobile',
      'offersAtHomeService': true,
      'atHomeServiceFee': 20.0,
      'maxTravelRangeKm': 15,
      'createdAt': FieldValue.serverTimestamp(),
      'updatedAt': FieldValue.serverTimestamp(),
    }, SetOptions(merge: true));

    return starterName;
  }

  Future<void> _verifyWithStripe() async {
    try {
      final result = await FirebaseFunctions.instance
          .httpsCallable('createIdentityVerificationSession')
          .call();
      final data = Map<String, dynamic>.from(result.data as Map);
      final url = data['url'] as String?;
      if (url == null || url.isEmpty) {
        throw Exception('Missing Stripe verification URL');
      }

      await launchUrl(Uri.parse(url), mode: LaunchMode.externalApplication);
      if (!mounted) return;
      setState(() => verificationStatus = 'PENDING');
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Could not start verification. Please try again.'),
        ),
      );
    }
  }

  Future<void> _saveChanges() async {
    final uid = _auth.currentUser?.uid;
    if (uid == null) return;

    if (serviceLocationType == 'mobile' && atHomeServiceFee < 0) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Enter a valid at-home travel fee.')),
      );
      return;
    }

    final radiusKm = (serviceRadius * 1.60934).round();
    await _firestore.collection('stylists').doc(uid).set({
      'name': name ?? _auth.currentUser?.displayName ?? 'Stylist',
      'email': _auth.currentUser?.email ?? '',
      'role': 'STYLIST',
      'serviceLocationType': serviceLocationType,
      'offersAtHomeService': serviceLocationType == 'mobile',
      'maxTravelRangeKm': radiusKm,
      'atHomeServiceFee': serviceLocationType == 'mobile'
          ? atHomeServiceFee
          : 0.0,
      'updatedAt': FieldValue.serverTimestamp(),
    }, SetOptions(merge: true));

    if (serviceLocationType == 'mobile') {
      await _firestore.collection('stylists').doc(uid).update({
        'location': FieldValue.delete(),
      });
    }

    if (!mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text('Profile changes saved.')));
  }

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).primaryColor;

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: Text(
          'Professional Profile',
          style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold),
        ),
        backgroundColor: Colors.white,
        elevation: 0,
        iconTheme: IconThemeData(color: Colors.black),
      ),
      body: _isLoadingProfile
          ? Center(child: CircularProgressIndicator())
          : _profileLoadError != null
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24.0),
                    child: Text(
                      _profileLoadError!,
                      textAlign: TextAlign.center,
                      style: TextStyle(color: Colors.grey[700]),
                    ),
                  ),
                )
              : SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Profile Image
              Center(
                child: Stack(
                  children: [
                    CircleAvatar(
                      radius: 60,
                      backgroundImage: profileImageUrl != null
                          ? NetworkImage(profileImageUrl!)
                          : null,
                      child: profileImageUrl == null
                          ? Icon(Icons.person, size: 60)
                          : null,
                    ),
                    Positioned(
                      bottom: 0,
                      right: 0,
                      child: CircleAvatar(
                        backgroundColor: Colors.black,
                        radius: 20,
                        child: Icon(Icons.star, color: Colors.amber, size: 24),
                      ),
                    ),
                  ],
                ),
              ),
              SizedBox(height: 20),

              // Name
              Center(
                child: Text(
                  name ?? 'Stylist Name',
                  style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                ),
              ),

              SizedBox(height: 30),
              Center(
                child: Text(
                  "PROFESSIONAL DASHBOARD",
                  style: TextStyle(
                    color: primaryColor,
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                    letterSpacing: 1.2,
                  ),
                ),
              ),
              SizedBox(height: 20),

              // Bio Card
              Card(
                elevation: 0,
                color: Colors.grey[100],
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Padding(
                  padding: EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        "Professional Bio",
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                        ),
                      ),
                      SizedBox(height: 12),
                      Container(
                        padding: EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: Colors.grey[300]!),
                        ),
                        child: Text(
                          bio ?? "Add a bio...",
                          style: TextStyle(fontSize: 16, height: 1.4),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              SizedBox(height: 16),

              // Location Card
              Card(
                elevation: 0,
                color: Colors.grey[100],
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Padding(
                  padding: EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: [
                          Row(
                            children: [
                              Radio<String>(
                                value: "fixed",
                                groupValue: serviceLocationType,
                                onChanged: (v) =>
                                    setState(() => serviceLocationType = v!),
                              ),
                              Text("Fixed\nLocation"),
                            ],
                          ),
                          Row(
                            children: [
                              Radio<String>(
                                value: "mobile",
                                groupValue: serviceLocationType,
                                onChanged: (v) =>
                                    setState(() => serviceLocationType = v!),
                              ),
                              Text("Mobile (I\ntravel to\nclients)"),
                            ],
                          ),
                        ],
                      ),
                      if (serviceLocationType == 'mobile') ...[
                        SizedBox(height: 12),
                        TextFormField(
                          initialValue: serviceRadius.toString(),
                          decoration: InputDecoration(
                            labelText: "Service Radius (miles)",
                          ),
                          keyboardType: TextInputType.number,
                          onChanged: (value) {
                            serviceRadius =
                                int.tryParse(value.trim()) ?? serviceRadius;
                          },
                        ),
                        SizedBox(height: 12),
                        TextFormField(
                          initialValue: atHomeServiceFee.toStringAsFixed(2),
                          decoration: InputDecoration(
                            labelText: "At-home travel fee (\$)",
                          ),
                          keyboardType: TextInputType.numberWithOptions(
                            decimal: true,
                          ),
                          onChanged: (value) {
                            atHomeServiceFee =
                                double.tryParse(value.trim()) ??
                                atHomeServiceFee;
                          },
                        ),
                      ],
                    ],
                  ),
                ),
              ),
              SizedBox(height: 16),

              // License & Identity Card
              Card(
                elevation: 0,
                color: Colors.grey[100],
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Padding(
                  padding: EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        "License & Identity",
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                        ),
                      ),
                      SizedBox(height: 16),
                      if (licenseImageUrl != null)
                        ClipRRect(
                          borderRadius: BorderRadius.circular(8),
                          child: Image.network(
                            licenseImageUrl!,
                            height: 180,
                            width: double.infinity,
                            fit: BoxFit.cover,
                          ),
                        ),
                      if (licenseImageUrl != null) SizedBox(height: 16),

                      Center(
                        child: Text(
                          isVerified
                              ? "Status: Verified Stylist"
                              : "Status: $verificationStatus",
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: isVerified ? Colors.green : Colors.red,
                          ),
                        ),
                      ),

                      if (!isVerified) ...[
                        SizedBox(height: 16),
                        SizedBox(
                          width: double.infinity,
                          height: 50,
                          child: ElevatedButton(
                            style: ElevatedButton.styleFrom(
                              backgroundColor: primaryColor,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                            ),
                            onPressed: _verifyWithStripe,
                            child: Text("Verify with Stripe"),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),

              SizedBox(height: 40),
              SizedBox(
                width: double.infinity,
                height: 60,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: primaryColor,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(30),
                    ),
                  ),
                  onPressed: _saveChanges,
                  child: Text(
                    "SAVE CHANGES",
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                ),
              ),
              SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }
}
