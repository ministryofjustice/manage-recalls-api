export AWS_ACCESS_KEY_ID=dontcare
export AWS_SECRET_ACCESS_KEY=dontcare

aws s3api create-bucket --bucket test-manage-recalls-api --region eu-west-2 --endpoint-url http://localhost:4566