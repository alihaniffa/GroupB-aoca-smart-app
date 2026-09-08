#import "FirebaseDatabaseBridge.h"

@implementation FirebaseDatabaseBridge

+ (void)observeValueOnceForQuery:(FIRDatabaseQuery *)query
                      completion:(void (^)(FIRDataSnapshot * _Nullable snapshot))completion
{
    [query observeSingleEventOfType:FIRDataEventTypeValue
                          withBlock:^(FIRDataSnapshot *snapshot) {
                              completion(snapshot);
                          }
                    withCancelBlock:^(NSError *error) {
                        NSLog(@"Firebase Database read failed: %@", error.localizedDescription);
                        completion(nil);
                    }];
}

@end