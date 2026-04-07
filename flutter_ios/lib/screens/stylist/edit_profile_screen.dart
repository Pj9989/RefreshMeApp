import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_stripe/flutter_stripe.dart';
import 'package:url_launcher/url_launcher.dart';

class EditProfileScreen extends StatefulWidget {
  final bool isStylist;

  const EditProfileScreen({Key? key, required this.isStylist}) : super(key: key);

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

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    final uid = _auth.currentUser?.uid;
    if (uid == null) return;

    final doc = await _firestore.collection('stylists').doc(uid).get();
    if (doc.exists) {
      final data = doc.data()!;
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
      });
    }
  }

  Future<void> _verifyWithStripe() async {
    // In a real app, call your Cloud Function here to get the ephemeral key and session ID.
    // Then launch Stripe Identity.
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Verification submitted. Please wait for confirmation.')),
    );
  }

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).primaryColor;

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: Text('Professional Profile', style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        elevation: 0,
        iconTheme: IconThemeData(color: Colors.black),
      ),
      body: SingleChildScrollView(
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
                      child: profileImageUrl == null ? Icon(Icons.person, size: 60) : null,
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
              Center(child: Text("PROFESSIONAL DASHBOARD", style: TextStyle(color: primaryColor, fontWeight: FontWeight.bold, fontSize: 16, letterSpacing: 1.2))),
              SizedBox(height: 20),

              // Bio Card
              Card(
                elevation: 0,
                color: Colors.grey[100],
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Padding(
                  padding: EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text("Professional Bio", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                      SizedBox(height: 12),
                      Container(
                        padding: EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: Colors.grey[300]!)
                        ),
                        child: Text(
                          bio ?? "Add a bio...",
                          style: TextStyle(fontSize: 16, height: 1.4),
                        ),
                      ),
                      SizedBox(height: 12),
                      Align(
                        alignment: Alignment.centerRight,
                        child: TextButton.icon(
                          onPressed: () {},
                          icon: Icon(Icons.auto_awesome, color: primaryColor),
                          label: Text("AI GENERATE BIO", style: TextStyle(color: primaryColor, fontWeight: FontWeight.bold)),
                        ),
                      )
                    ],
                  ),
                ),
              ),
              SizedBox(height: 16),

              // Location Card
              Card(
                elevation: 0,
                color: Colors.grey[100],
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Padding(
                  padding: EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: [
                          Row(children: [
                            Radio(value: "fixed", groupValue: serviceLocationType, onChanged: (v){}),
                            Text("Fixed\nLocation")
                          ]),
                          Row(children: [
                            Radio(value: "mobile", groupValue: serviceLocationType, onChanged: (v){}),
                            Text("Mobile (I\ntravel to\nclients)")
                          ]),
                        ],
                      ),
                      if (serviceLocationType == 'mobile') ...[
                        SizedBox(height: 12),
                        TextFormField(
                          initialValue: serviceRadius.toString(),
                          decoration: InputDecoration(
                            labelText: "Service Radius (miles)",
                          ),
                        )
                      ]
                    ],
                  ),
                ),
              ),
              SizedBox(height: 16),

              // License & Identity Card
              Card(
                elevation: 0,
                color: Colors.grey[100],
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Padding(
                  padding: EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text("License & Identity", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                      SizedBox(height: 16),
                      if (licenseImageUrl != null)
                        ClipRRect(
                          borderRadius: BorderRadius.circular(8),
                          child: Image.network(licenseImageUrl!, height: 180, width: double.infinity, fit: BoxFit.cover),
                        ),
                      SizedBox(height: 16),
                      SizedBox(
                        width: double.infinity,
                        height: 50,
                        child: ElevatedButton(
                          style: ElevatedButton.styleFrom(
                            backgroundColor: primaryColor,
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))
                          ),
                          onPressed: () {},
                          child: Text("Update License"),
                        ),
                      ),
                      SizedBox(height: 16),

                      Center(
                        child: Text(
                          isVerified ? "Status: Verified Stylist" : "Status: $verificationStatus",
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: isVerified ? Colors.green : Colors.red
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
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))
                            ),
                            onPressed: _verifyWithStripe,
                            child: Text("Verify with Stripe"),
                          ),
                        ),
                      ]
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
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30))
                  ),
                  onPressed: () {},
                  child: Text("SAVE CHANGES", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
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
