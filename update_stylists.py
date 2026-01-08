import firebase_admin
from firebase_admin import credentials, firestore
import random
import sys

try:
    cred = credentials.Certificate('refreshme-74f79-firebase-adminsdk-ufe8z-9eebaeb752.json')
    firebase_admin.initialize_app(cred)
    db = firestore.client()
    print("\n✅ Connected to Firebase!\n")
except Exception as e:
    print(f"❌ Error: {e}")
    sys.exit(1)

SERVICES_BY_SPECIALTY = {
    "Beard Trims": [
        {"name": "Classic Beard Trim", "price": 25, "duration": 30},
        {"name": "Beard Shape & Style", "price": 35, "duration": 45},
        {"name": "Full Beard Grooming", "price": 45, "duration": 60},
    ],
    "Haircuts": [
        {"name": "Men's Haircut", "price": 30, "duration": 45},
        {"name": "Fade Haircut", "price": 35, "duration": 50},
        {"name": "Haircut & Beard Trim", "price": 50, "duration": 75},
        {"name": "Kids Haircut", "price": 20, "duration": 30},
    ],
    "Hair Coloring": [
        {"name": "Full Color", "price": 80, "duration": 120},
        {"name": "Highlights", "price": 100, "duration": 150},
        {"name": "Balayage", "price": 120, "duration": 180},
        {"name": "Root Touch-Up", "price": 60, "duration": 90},
    ],
    "Styling": [
        {"name": "Blow Dry & Style", "price": 40, "duration": 45},
        {"name": "Special Event Styling", "price": 75, "duration": 90},
        {"name": "Updo", "price": 85, "duration": 90},
    ],
    "Hair Treatments": [
        {"name": "Deep Conditioning", "price": 50, "duration": 60},
        {"name": "Keratin Treatment", "price": 200, "duration": 180},
        {"name": "Scalp Treatment", "price": 45, "duration": 45},
    ],
    "Braiding": [
        {"name": "Box Braids", "price": 150, "duration": 240},
        {"name": "Cornrows", "price": 80, "duration": 120},
        {"name": "Senegalese Twists", "price": 180, "duration": 300},
    ],
}

ATLANTA_ADDRESSES = [
    "123 Peachtree St NE, Atlanta, GA 30303",
    "456 Piedmont Ave NE, Atlanta, GA 30308",
    "789 Ponce De Leon Ave NE, Atlanta, GA 30306",
    "321 Edgewood Ave SE, Atlanta, GA 30312",
    "654 North Highland Ave NE, Atlanta, GA 30306",
    "987 Marietta St NW, Atlanta, GA 30318",
    "147 Decatur St SE, Atlanta, GA 30312",
    "258 Auburn Ave NE, Atlanta, GA 30303",
    "369 Memorial Dr SE, Atlanta, GA 30312",
    "741 Moreland Ave SE, Atlanta, GA 30316",
]

SAMPLE_BIOS = [
    "Passionate about creating the perfect look for every client. With years of experience, I specialize in modern styles and classic cuts.",
    "Your hair is your crown - let me help you wear it with pride! I love working with all hair types and textures.",
    "Professional stylist dedicated to making you look and feel your best. Book your appointment today!",
    "Experienced in the latest trends and techniques. I take pride in every cut, color, and style.",
    "Creating beautiful hair transformations is my passion. Let's work together to achieve your dream look!",
    "Specializing in personalized service and attention to detail. Your satisfaction is my priority.",
    "Bringing creativity and expertise to every appointment. I can't wait to work with you!",
    "Professional, friendly, and always up-to-date with the latest styles. Let's make magic happen!",
]

print("🔄 Updating stylist profiles...\n")

stylists_ref = db.collection('stylistProfiles')
stylists = stylists_ref.limit(15).stream()

updated_count = 0

for stylist in stylists:
    stylist_id = stylist.id
    stylist_data = stylist.to_dict()
    
    print(f"📝 {stylist_data.get('name', 'Unknown')}")
    
    update_data = {}
    
    if not stylist_data.get('services'):
        specialty = stylist_data.get('specialty', 'Haircuts')
        services = SERVICES_BY_SPECIALTY.get(specialty, SERVICES_BY_SPECIALTY['Haircuts'])
        update_data['services'] = services
        print(f"  ✅ Added {len(services)} services")
    
    if not stylist_data.get('bio'):
        update_data['bio'] = random.choice(SAMPLE_BIOS)
        print(f"  ✅ Added bio")
    
    if not stylist_data.get('address'):
        update_data['address'] = random.choice(ATLANTA_ADDRESSES)
        print(f"  ✅ Added address")
    
    if stylist_data.get('offersAtHomeService') is None:
        update_data['offersAtHomeService'] = True
        update_data['atHomeServiceFee'] = random.choice([15, 20, 25, 30])
        print(f"  ✅ At-home service: ${update_data['atHomeServiceFee']}")
    
    if not stylist_data.get('yearsOfExperience'):
        update_data['yearsOfExperience'] = random.randint(3, 15)
        print(f"  ✅ Experience: {update_data['yearsOfExperience']} years")
    
    if not stylist_data.get('availableNow'):
        update_data['availableNow'] = random.choice([True, True, False])
        if update_data['availableNow']:
            print(f"  ✅ Available now")
    
    if update_data:
        stylists_ref.document(stylist_id).update(update_data)
        updated_count += 1
        print(f"  ✨ Updated!\n")
    else:
        print(f"  ℹ️  Already complete\n")

print(f"\n{'='*60}")
print(f"🎉 Updated {updated_count} stylist profiles!")
print(f"{'='*60}\n")