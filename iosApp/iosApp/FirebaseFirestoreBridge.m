#import "FirebaseFirestoreBridge.h"

@implementation FirebaseFirestoreBridge

+ (void)addActivityForUser:(NSString *)userId
                      data:(NSDictionary *)data
                completion:(void (^)(NSError * _Nullable error))completion
{
    FIRFirestore *db = [FIRFirestore firestore];

    FIRCollectionReference *activitiesRef =
            [[[db collectionWithPath:@"UserProfiles"]
                    documentWithPath:userId]
                    collectionWithPath:@"activities"];

    [activitiesRef addDocumentWithData:data
                            completion:^(NSError * _Nullable error) {
                                completion(error);
                            }];
}

+ (void)getActivitiesForUser:(NSString *)userId
                  completion:(void (^)(NSArray<NSDictionary *> * _Nullable activities,
NSError * _Nullable error))completion
{
    FIRFirestore *db = [FIRFirestore firestore];

    FIRCollectionReference *activitiesRef =
            [[[db collectionWithPath:@"UserProfiles"]
                    documentWithPath:userId]
                    collectionWithPath:@"activities"];

    FIRQuery *query =
            [activitiesRef queryOrderedByField:@"timestamp"
                                    descending:YES];

    [query getDocumentsWithCompletion:^(FIRQuerySnapshot * _Nullable snapshot,
            NSError * _Nullable error) {

        if (error != nil) {
            completion(nil, error);
            return;
        }

        NSMutableArray<NSDictionary *> *activities =
                [NSMutableArray array];

        for (FIRDocumentSnapshot *document in snapshot.documents) {
            NSMutableDictionary *activity =
                    [document.data mutableCopy];

            activity[@"_documentId"] = document.documentID;

            [activities addObject:activity];
        }

        completion(activities, nil);
    }];
}

+ (void)getActivitiesForUser:(NSString *)userId
               fromTimestamp:(int64_t)timestamp
                  completion:(void (^)(NSArray<NSDictionary *> * _Nullable activities,
NSError * _Nullable error))completion
{
    FIRFirestore *db = [FIRFirestore firestore];

    FIRCollectionReference *activitiesRef =
            [[[db collectionWithPath:@"UserProfiles"]
                    documentWithPath:userId]
                    collectionWithPath:@"activities"];

    FIRQuery *query =
            [activitiesRef queryWhereField:@"timestamp"
                    isGreaterThanOrEqualTo:@(timestamp)];

    query =
            [query queryOrderedByField:@"timestamp"
                            descending:YES];

    [query getDocumentsWithCompletion:^(FIRQuerySnapshot * _Nullable snapshot,
            NSError * _Nullable error) {

        if (error != nil) {
            completion(nil, error);
            return;
        }

        NSMutableArray<NSDictionary *> *activities =
                [NSMutableArray array];

        for (FIRDocumentSnapshot *document in snapshot.documents) {
            NSMutableDictionary *activity =
                    [document.data mutableCopy];

            activity[@"_documentId"] = document.documentID;

            [activities addObject:activity];
        }

        completion(activities, nil);
    }];
}

@end